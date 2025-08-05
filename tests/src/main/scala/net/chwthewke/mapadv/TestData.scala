package net.chwthewke.mapadv

import cats.data.NonEmptyVector
import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.readClassLoaderResource
import io.circe.Decoder
import io.circe.parser
import scala.collection.immutable.SortedMap

object TestData:
  def load[F[_]]( using F: Async[F] ): F[MapData] =
    readClassLoaderResource( "mapdata.json" )
      .through( fs2.text.utf8.decode )
      .compile
      .foldMonoid
      .flatTap( _ => F.realTimeInstant.flatMap( t => Console.make[F].println( s"$t Read raw map data" ) ) )
      .flatMap( str => parser.decode[MapData]( str ).liftTo[F] )
      .flatTap( d =>
        F.realTimeInstant
          .flatMap( t => Console.make[F].println( s"$t Read map data with ${d.townships.size} townships" ) )
      )

  private def loadJson[F[_]: Async, A: Decoder]( name: String ): F[A] =
    readClassLoaderResource( name )
      .through( fs2.text.utf8.decode )
      .compile
      .foldMonoid
      .flatMap( str => parser.decode[A]( str ).liftTo[F] )
      .adaptError( err => new RuntimeException( s"Error decoding $name", err ) )

  private def loadTownships[F[_]: Async]( i: Int ): F[Map[InseeCode, Township]] =
    loadJson[F, Vector[Township]]( s"townships_$i.json" )
      .map( ts => ts.fproductLeft( _.code ).toMap )

  private def loadAdjacencies[F[_]: Async]( i: Int ): F[Map[InseeCode, NonEmptyVector[InseeCode]]] =
    loadJson[F, Vector[( InseeCode, NonEmptyVector[InseeCode] )]]( s"adjacencies_$i.json" )
      .map( ts => ts.toMap )

  private def loadComponent[F[_]: Async]( i: Int ): F[Component] =
    loadJson[F, Component]( s"components_$i.json" )

  def loadSplit[F[_]]( using F: Async[F] ): F[MapData] =
    for
      index     <- loadJson[F, MapIndex]( "index.json" )
      townships <- ( 0 until index.parts ).toVector.foldLeftM( SortedMap.empty[InseeCode, Township] )( ( acc, i ) =>
                     loadTownships( i ).map( acc ++ _ )
                   )
      adjacencies <- ( 0 until index.parts ).toVector.foldLeftM( Map.empty[InseeCode, NonEmptyVector[InseeCode]] )(
                       ( acc, i ) => loadAdjacencies( i ).map( acc ++ _ )
                     )
      
      componentsV <- ( 0 until index.components ).toVector.foldMapM( i => loadComponent( i ).map( Vector( _ ) ) )
      components  <- componentsV.toNev.liftTo[F]( new NoSuchElementException( "zero components" ) )
    yield MapData( townships, components, adjacencies )
