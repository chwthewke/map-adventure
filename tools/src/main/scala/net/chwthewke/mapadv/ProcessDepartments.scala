package net.chwthewke.mapadv

import cats.data.NonEmptyChain
import cats.data.NonEmptySet
import cats.data.NonEmptyVector
import cats.data.OptionT
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all.*
import cats.effect.std.Console
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import io.circe.Decoder
import io.circe.Encoder
import io.circe.parser
import io.circe.syntax.*
import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.concurrent.duration.FiniteDuration

private class Segment( val p1: ApiPoint, val p2: ApiPoint ):
  override def hashCode(): Int = p1.hashCode() + p2.hashCode()

  override def equals( obj: Any ): Boolean =
    if ( !obj.isInstanceOf[Segment] ) false
    else
      val other: Segment = obj.asInstanceOf[Segment]
      other.p1 == p1 && other.p2 == p2 ||
      other.p1 == p2 && other.p2 == p1

class ProcessDepartments[F[_]]( using F: Async[F], files: Files[F] ):
  private val console: Console[F] = Console.make[F]

  private inline def decodeDepartment( code: DepartmentCode ): Decoder[Vector[ApiTownship]] =
    given Decoder[ApiTownship] = Decoder[DepartmentCode => ApiTownship].map( _( code ) )
    summon[Decoder[Vector[ApiTownship]]]

  def readDepartment( dir: Path, code: DepartmentCode ): F[Vector[ApiTownship]] =
    files
      .readUtf8( dir / s"${code.code}.json" )
      .compile
      .foldMonoid
      .map( parser.decode( _ )( using decodeDepartment( code ) ) )
      .rethrow
      .flatTap( ts => console.println( s"Read ${ts.length} townships for department $code" ) )

  def readDepartments( options: Options ): F[Vector[ApiTownship]] =
    options.departmentCodes
      .foldMapM( readDepartment( options.targetDirectory, _ ) )
      .flatTap( ts => console.println( s"Read ${ts.length} departments in total" ) )

  def findAdjacencies( townships: Vector[ApiTownship] ): F[Map[InseeCode, NonEmptyVector[InseeCode]]] =
    val bySegment =
      townships.foldMap: township =>
        township.contour.rings
          .foldMap( _.segments.map { case ( p, q ) => Segment( p, q ) } )
          .foldMap( segment => Map( segment -> NonEmptyChain.one( township.insee ) ) )

    console.println( s"Arranged ${bySegment.size} segments" ) *>
      {
        val adjacencies: Map[InseeCode, NonEmptyVector[InseeCode]] = bySegment
          .unorderedFoldMap( neighbours =>
            neighbours.toList match
              case t :: s :: Nil => Map( t -> NonEmptySet.one( s ), s -> NonEmptySet.one( t ) )
              case _             => Map.empty
          )
          .fmap( nes => NonEmptyVector( nes.head, nes.tail.toVector ) )

        val ( c, t ) = adjacencies.unorderedFoldMap( ns => ( 1, ns.size ) )

        val n = t / c.toDouble

        console
          .println( f"Computed adjacencies for $c%d elements, $n%.2f avg. neighbours" )
          .as( adjacencies )
      }

  def extractComponent(
      adjacencies: Map[InseeCode, NonEmptyVector[InseeCode]],
      start: InseeCode
  ): F[NonEmptyVector[InseeCode]] =

    @tailrec
    def loop1( seen: NonEmptySet[InseeCode], open: Set[InseeCode] ): NonEmptyVector[InseeCode] =
      open.headOption match
        case None         => seen.toNonEmptyList.toNev
        case Some( next ) =>
          val neighbours: Vector[InseeCode] =
            adjacencies.get( next ).fold( Vector.empty )( _.filterNot( seen.contains_ ) )
          val newSeen = neighbours.foldLeft( seen )( _.add( _ ) )
          val newOpen = open.tail ++ neighbours
          loop1( newSeen, newOpen )

    val result = loop1( NonEmptySet.one( start ), Set( start ) )
    console.println( show"Found component with ${result.size} elements at $start" ).as( result )

  def components(
      adjacencies: Map[InseeCode, NonEmptyVector[InseeCode]],
      townships: Vector[ApiTownship]
  ): F[Vector[NonEmptyVector[InseeCode]]] =
    def loop(
        args: ( Map[InseeCode, NonEmptyVector[InseeCode]], Vector[NonEmptyVector[InseeCode]] )
    ): F[Either[
      ( Map[InseeCode, NonEmptyVector[InseeCode]], Vector[NonEmptyVector[InseeCode]] ),
      Vector[NonEmptyVector[InseeCode]]
    ]] =
      val (
        remainingAdjacencies: Map[InseeCode, NonEmptyVector[InseeCode]],
        completeComponents: Vector[NonEmptyVector[InseeCode]]
      ) = args

      remainingAdjacencies.headOption match
        case None                => Right( completeComponents ).pure[F]
        case Some( ( code, _ ) ) =>
          extractComponent( adjacencies, code )
            .flatMap: newComponent =>
              val newAdjacencies = remainingAdjacencies.removedAll( newComponent.toVector )
              console
                .println( s"  ${newAdjacencies.size} adjacencies remaining" )
                .as( Left( ( newAdjacencies, completeComponents :+ newComponent ) ) )

    ( adjacencies, Vector.empty[NonEmptyVector[InseeCode]] )
      .tailRecM( loop )
      .map( _ ++ townships.map( _.insee ).filterNot( adjacencies.contains ).map( NonEmptyVector.one ) )

  def printElapsed( job: String )( from: FiniteDuration ): F[FiniteDuration] =
    F.monotonic.flatTap: now =>
      val elapsedMillis = now.minus( from ).toMillis
      console.println( s"$elapsedMillis ms: $job" )

  def process( options: Options ): F[Option[MapData]] =
    for
      t0          <- F.monotonic
      townships   <- readDepartments( options )
      t1          <- printElapsed( "Read departments" )( t0 )
      adjacencies <- findAdjacencies( townships )
      t2          <- printElapsed( "Compute adjacencies" )( t1 )
      comps       <- components( adjacencies, townships )
      _           <- printElapsed( "Compute components" )( t2 )
      _           <- printElapsed( "Total" )( t0 )
    yield comps.toNev.map(
      MapData.of(
        townships.map( t => ( t.insee, t.data ) ).to( SortedMap ),
        _,
        adjacencies
      )
    )

  def run( options: Options ): F[Unit] =
    OptionT( process( options ) )
      .getOrRaise( new IllegalStateException( "Empty data" ) )
      .flatTap: data =>
        val tc: Int      = data.townships.size
        val avgN: Double = data.adjacencies.unorderedFoldMap( _.length ) / tc.toDouble
        val maxC: Int    = data.components.map( _.townships.length ).maximum
        val numC: Int    = data.components.length
        console.println(
          show"""Read $tc townships
                |Average of ${f"$avgN%.2f"} neighbours
                |$numC components
                |  - largest with $maxC elements
                |""".stripMargin
        )
      .flatMap( data => writeMapData( 64, data ) )

  private def writeJson[A: Encoder]( name: String, data: A ): F[Unit] =
    Stream
      .emit( data.asJson.noSpaces )
      .through( files.writeUtf8( Path( "." ) / "server" / "src" / "main" / "resources" / name ) )
      .compile
      .drain

  def writeMapData( parts: Int, data: MapData ): F[Unit] =
    val townships: SortedMap[Int, Vector[Township]] =
      data.townships.foldMap: t =>
        SortedMap( ( t.code.hashCode & 0x7fffffff ) % parts -> Vector( t ) )
    val adjacencies = data.adjacencies.toVector.foldMap:
      case ( code, neighbours ) =>
        SortedMap( ( code.hashCode & 0x7fffffff ) % parts -> Vector( ( code, neighbours ) ) )
    townships.toVector.traverse_ :
      case ( i, ts ) => writeJson( s"townships_$i.json", ts )
    *> data.components.zipWithIndex.traverse_ :
      case ( c, i ) => writeJson( s"components_$i.json", c )
    *> adjacencies.toVector.traverse_ :
      case ( i, as ) => writeJson( s"adjacencies_$i.json", as )
    *> writeJson( "index.json", MapIndex( parts, data.components.length ) )

object ProcessDepartments extends IOApp:
  override def run( args: List[String] ): IO[ExitCode] =
    new ProcessDepartments[IO].run( Options() ).as( ExitCode.Success )
