package net.chwthewke.mapadv
package spa

import cats.data.NonEmptyVector
import cats.effect.Async
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.Uri
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.client.Middleware
import org.http4s.dom.FetchClientBuilder
import scala.collection.immutable.SortedMap
import tyrian.Cmd

enum AppModel[F[_]]:
  case Error( message: String )
  case Loading( http: Http[F] )
  case Loaded( http: Http[F], mapData: MapData )

object AppModel:

  def error[F[_]]( message: String ): ( AppModel[F], Cmd[F, Nothing] ) = ( Error( message ), Cmd.None )

  def init[F[_]: Async]( flags: Map[String, String] ): ( AppModel[F], Cmd[F, Msg] ) =
    flags
      .get( "backend" )
      .toRight( "Missing flag 'backend'" )
      .flatMap( Uri.fromString( _ ).leftMap( _.message ) )
      .map( Http.init[F]( _ ) )
      .fold(
        error[F],
        http => Loading( http ) -> http.loadMapData
      )

  extension [F[_]]( model: AppModel[F] )
    def update( msg: Msg ): ( AppModel[F], Cmd[F, Msg] ) =
      model match
        case AppModel.Error( message ) => ( model, Cmd.None )
        case AppModel.Loading( http )  =>
          msg match
            case Msg.Noop                   => ( model, Cmd.None )
            case Msg.ReceiveMapData( data ) => ( Loaded( http, data ), Cmd.None )
        case AppModel.Loaded( http, mapData ) => ( model, Cmd.None )

class Http[F[_]: Async]( val backend: Uri, private val client: Client[F] ):
  def loadJson[A: Decoder]( name: String )( using Async[F] ): F[A] =
    client.expect[A]( backend / "static" / name )

  private def loadTownships( i: Int ): F[Map[InseeCode, Township]] =
    loadJson[Vector[Township]]( s"townships_$i.json" )
      .map( ts => ts.fproductLeft( _.code ).toMap )

  private def loadAdjacencies( i: Int ): F[Map[InseeCode, NonEmptyVector[InseeCode]]] =
    loadJson[Vector[( InseeCode, NonEmptyVector[InseeCode] )]]( s"adjacencies_$i.json" )
      .map( ts => ts.toMap )

  private def loadComponent( i: Int ): F[Component] =
    loadJson[Component]( s"components_$i.json" )

  def loadSplit: F[MapData] =
    for
      index     <- loadJson[MapIndex]( "index.json" )
      townships <- ( 0 until index.parts ).toVector.foldLeftM( SortedMap.empty[InseeCode, Township] )( ( acc, i ) =>
                     loadTownships( i ).map( acc ++ _ )
                   )
      adjacencies <- ( 0 until index.parts ).toVector.foldLeftM( Map.empty[InseeCode, NonEmptyVector[InseeCode]] )(
                       ( acc, i ) => loadAdjacencies( i ).map( acc ++ _ )
                     )
      componentsV <- ( 0 until index.components ).toVector.foldMapM( i => loadComponent( i ).map( Vector( _ ) ) )
      components  <- componentsV.toNev.liftTo[F]( new NoSuchElementException( "zero components" ) )
    yield MapData( townships, components, adjacencies )

  val loadMapData: Cmd[F, Msg] =
    Cmd.Run( loadSplit.map( Msg.ReceiveMapData( _ ) ) )

object Http:
  def init[F[_]: Async]( backend: Uri ): Http[F] =
    new Http( backend, middleware[F]( backend )( FetchClientBuilder[F].create ) )

  private def middleware[F[_]: Async]( backend: Uri ): Middleware[F] = client =>
    val slashed: Uri = backend.withPath( backend.path.addEndsWithSlash )
    Client[F]( req => client.run( req.withUri( slashed.resolve( req.uri ) ) ) )
