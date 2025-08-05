package net.chwthewke.mapadv
package spa

import tyrian.Html

import spa.css.Bulma

object MainView:
  val b: Bulma = Bulma

  def apply[F[_]]( model: AppModel[F] ): Html[Msg] =
    model match
      case AppModel.Error( message ) =>
        Html.h1( b.title + b.isDanger )( message )
      case AppModel.Loading( http ) =>
        Html.h1( b.title + b.isInfo )( s"Loading data from ${http.backend.renderString}" )
      case AppModel.Loaded( http, mapData ) =>
        Html.p(
          Html.text( "Loaded map data" ),
          Html.br(),
          Html.text( s"${mapData.townships.size} townships" )
        )
