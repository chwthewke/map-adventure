package net.chwthewke.mapadv
package spa

import cats.effect.Async
import cats.effect.IO
import scala.scalajs.js.annotation.JSExportTopLevel
import tyrian.Cmd
import tyrian.Html
import tyrian.Location
import tyrian.Sub
import tyrian.TyrianApp
import tyrian.TyrianIOApp

type Msg   = String
type Model = String

abstract class Main[F[_]: Async] extends TyrianApp[F, Msg, Model]:
  override def router: Location => Msg = _ => ""

  override def init( flags: Map[String, String] ): ( Model, Cmd[F, Msg] ) =
    ( flags.getOrElse( "backend", "missing backend flag" ), Cmd.None )

  override def update( model: Model ): Msg => ( Model, Cmd[F, Msg] ) = _ => ( model, Cmd.None )

  override def view( model: Model ): Html[Msg] = Html.div(
    Html.h1( Library.function.toString ),
    Html.div( s"Tyrian app ${MapAdventure.name} version ${MapAdventure.version} started" ),
    Html.div( s"backend url: $model" )
  )

  override def subscriptions( model: Model ): Sub[F, Msg] = Sub.None

@JSExportTopLevel( "TyrianApp" )
object Main extends Main[IO] with TyrianIOApp[Msg, Model]
