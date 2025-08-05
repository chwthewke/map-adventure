package net.chwthewke.mapadv

import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.Console
import cats.syntax.all.*

class DistanceTest[F[_]: Async]( val mapData: MapData ):
  private val console: Console[F] = Console.make[F]

  def insert[A, B]( elem: A, by: A => B, into: List[A] )( using Ordering[B] ): List[A] =
    import Ordering.Implicits.*
    into match
      case head :: tail =>
        if ( by( elem ) <= by( head ) )
          elem :: into
        else head :: insert( elem, by, tail )
      case Nil =>
        elem :: Nil

  def kLargest[A, B]( k: Int, from: Iterable[A] )( by: A => B )( using Ordering[B] ): List[A] =
    from.zipWithIndex.foldLeft( List.empty[A] ) {
      case ( acc, ( item, i ) ) =>
        if ( i % 1000 == 1 )
          println( s"done ${i - 1}" )
        insert( item, by, acc ).takeRight( k )
    }

  val largestCities: List[Township] =
    kLargest( 10, mapData.townships.values )( _.population ).reverse

  def run: F[Unit] =
    for
      _ <- console.println(
             largestCities
               .map( t => s"${t.name}, pop ${t.population}" )
               .mkString(
                 "Computing distances for\n  - ",
                 "\n  - ",
                 ""
               )
           )
      _ <- largestCities.zipWithIndex
             .mproduct { case ( _, i ) => largestCities.drop( i + 1 ) }
             .traverse_ {
               case ( ( c, _ ), d ) =>
                 println( s"${c.name} ${d.name}" )
                 val dist: Double = Distance( c.center, d.center )
                 console.println(
                   show"""Distance between
                         |    ${c.name} ${c.center}
                         |and ${d.name} ${d.center}
                         |  = $dist
                         |""".stripMargin
                 )
             }
    yield ()

object DistanceTest extends IOApp:
  override def run( args: List[String] ): IO[ExitCode] =
    TestData
      .loadSplit[IO]
      .flatMap( data => new DistanceTest[IO]( data ).run )
      .as( ExitCode.Success )
