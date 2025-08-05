package net.chwthewke.mapadv
package spa.games

import cats.Monad
import cats.effect.Async
import cats.effect.std.Console
import cats.effect.std.Random
import cats.syntax.all.*
import tyrian.Cmd
import tyrian.Html

import net.chwthewke.mapadv.spa.css.Bulma
import net.chwthewke.mapadv.spa.css.Classes
import net.chwthewke.mapadv.spa.games.DistanceGame.Model.Question

object DistanceGame:
  enum Model:
    case Init

    case Question(
        from: Township,
        targets: Vector[Township]
    )

    case Answer(
        from: Township,
        targets: Vector[( Township, Double )],
        guessed: Township,
        solution: Township
    )

  enum Msg:
    case Start
    case Ask( question: Model.Question )
    case Reply( code: InseeCode )

  def randomBoundedDistinct[F[_]: Monad]( seen: Set[Int], max: Int )( using random: Random[F] ): F[Int] =
    random
      .nextIntBounded( max )
      .flatMap( n =>
        n.tailRecM( x =>
          if ( seen( x ) ) random.nextIntBounded( max ).map( Left( _ ) )
          else Right( x ).pure[F]
        )
      )

  def pick[F[_]: Async]( count: Int, max: Int )( using random: Random[F] ): F[Vector[Int]] =
    ( count, Set.empty[Int] ).tailRecM:
      case ( c, seen ) =>
        if ( c == 0 ) Right( seen.toVector ).pure[F]
        else randomBoundedDistinct( seen, max ).map( n => Left( ( c - 1, seen + n ) ) )

  def randomQuestion[F[_]: Async]( data: MapData ): F[Model.Question] =
    val sample: Vector[Township] =
      data.townships.values.filter( _.population >= 5000 ).toVector
    val console = Console.make[F]
    Random
      .scalaUtilRandom[F]
      .flatTap( _ => console.println( "Starting randomQuestion" ) )
      .flatMap[Model.Question]: random =>
        given Random[F] = random
        pick( 6, sample.size ).map: picked =>
          val pickedTownships = picked.map( sample( _ ) )
          Model.Question( pickedTownships.head, pickedTownships.tail )
      .flatTap( q => console.println( s"Done randomQuestion: $q" ) )

  val b: Bulma = Bulma

  def view( model: Model ): Html[Msg] =
    model match
      case Model.Init                      => Html.div()
      case Model.Question( from, targets ) =>
        Html.div(
          Html.h2( "Find the closest town from" ),
          Html.p( from.name ),
          Html.div( b.buttons )(
            targets.toList.map( t =>
              Html.button( b.button + b.isPrimary, Html.onClick( Msg.Reply( t.code ) ) )( t.name )
            )
          )
        )
      case Model.Answer( from, targets, guessed, solution ) =>
        Html.div(
          Html.p( s"Closest towns from ${from.name} (${from.department}, pop. ${from.population}):" ),
          Html.ul(
            targets
              .sortBy( _._2 )
              .toList
              .map:
                case ( t, d ) =>
                  val ok: Boolean = solution.code == guessed.code

                  val classes: Classes =
                    Option.when[Classes]( t.code == solution.code )( b.hasTextWeightBold ).orEmpty |+|
                      Option
                        .when[Classes]( t.code == guessed.code )( if ( ok ) b.hasTextSuccess else b.hasTextDanger )
                        .orEmpty
                  Html.li( classes )( f"${d / 1000}%.2f km ${t.name} (${t.department}, pop. ${t.population})" )
          )
        )

  def update[F[_]: Async]( data: MapData, game: Option[Model], msg: Msg ): ( Option[Model], Cmd[F, Msg] ) =
    msg match
      case Msg.Start           => ( Some( Model.Init ), Cmd.Run( randomQuestion[F]( data ) ).map( Msg.Ask( _ ) ) )
      case Msg.Ask( question ) => ( Some( question ), Cmd.None )
      case Msg.Reply( code )   =>
        game match
          case Some( Model.Question( from, targets ) ) =>
            val targetsWithDistance: Vector[( Township, Double )] =
              targets.fproduct( t => Distance( from.center, t.center ) )

            (
              ( targets.find( _.code == code ), targetsWithDistance.minByOption( _._2 )._1F )
                .mapN: ( guessed, actual ) =>
                  Model.Answer(
                    from,
                    targetsWithDistance,
                    guessed,
                    actual
                  ),
              Cmd.None
            )
          case _ => ( game, Cmd.None )
