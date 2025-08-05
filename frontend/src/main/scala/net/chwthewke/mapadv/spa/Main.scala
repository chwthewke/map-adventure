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

type Model = String

abstract class Main[F[_]: Async] extends TyrianApp[F, Msg, AppModel[F]]:
  override def router: Location => Msg = _ => Msg.Noop

  override def init( flags: Map[String, String] ): ( AppModel[F], Cmd[F, Msg] ) =
    AppModel.init( flags )

  override def update( model: AppModel[F] ): Msg => ( AppModel[F], Cmd[F, Msg] ) =
    msg => model.update( msg )

  override def view( model: AppModel[F] ): Html[Msg] = Html.div(
    Html.div( s"Tyrian app ${MapAdventure.name} version ${MapAdventure.version} started" ),
    MainView.apply( model )
  )

  override def subscriptions( model: AppModel[F] ): Sub[F, Msg] = Sub.None

@JSExportTopLevel( "TyrianApp" )
object Main extends Main[IO] with TyrianIOApp[Msg, AppModel[IO]]
