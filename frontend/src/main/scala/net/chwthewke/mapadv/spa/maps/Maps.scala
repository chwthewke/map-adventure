package net.chwthewke.mapadv
package spa
package maps

import cats.data.NonEmptyVector
import cats.effect.Async
import cats.syntax.all.*
import scala.scalajs.js
import tyrian.Cmd

object Maps:
  private val L = js.Dynamic.global.L

  private def pointToJs( point: Point ): js.Array[Float] =
    js.Array( point.lat.toFloat, point.lng.toFloat )

  private def setTileLayer( map: js.Dynamic ): js.Dynamic =
    L
      .tileLayer(
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        js.Dictionary(
          "maxZoom"     -> 19,
          "attribution" -> """&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>"""
        )
      )
      .addTo( map )

  private def addMarker( map: js.Dynamic, township: Township ) =
    L.marker( pointToJs( township.center ) ).addTo( map ).bindPopup( township.name ).openPopup()

  def mapAtAll[F[_]]( using F: Async[F] )( townships: NonEmptyVector[Township] ): F[Unit] =
    val minBound =
      Point(
        townships.map( _.center.lat ).minimum,
        townships.map( _.center.lng ).minimum
      )
    val maxBound =
      Point(
        townships.map( _.center.lat ).maximum,
        townships.map( _.center.lng ).maximum
      )

    val bounds = js.Array( pointToJs( minBound ), pointToJs( maxBound ) )

    F.delay:
      @annotation.unused
      val map = L.map( "map" ).fitBounds( bounds )

      setTileLayer( map )

      townships.toVector.foreach: township =>
        addMarker( map, township )
    .void

  def mapAtAllCmd[F[_]: Async]( townships: NonEmptyVector[Township] ): Cmd[F, Nothing] =
    Cmd.SideEffect( mapAtAll( townships ) )

  def mapAt[F[_]]( using F: Async[F] )( township: Township ): F[Unit] =
    F.delay:
      val posJs = pointToJs( township.center )
      val map   = L.map( "map" ).setView( posJs, 10 )

      setTileLayer( map )

      addMarker( map, township )
    .void

  def mapAtCmd[F[_]: Async]( township: Township ): Cmd[F, Nothing] =
    Cmd.SideEffect( mapAt( township ) )

  def sampleMap[F[_]]( using F: Async[F] ): F[Unit] =
    F.delay:
      val map = js.Dynamic.global.L.map( "map" ).setView( js.Array( 51.505, -0.09 ), 13 )
      js.Dynamic.global.L
        .tileLayer(
          "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
          js.Dictionary(
            "maxZoom"     -> 19,
            "attribution" -> """&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>"""
          )
        )
        .addTo( map )
    .void

  def sampleMapCmd[F[_]: Async]: Cmd[F, Msg] =
    Cmd.Run( sampleMap.as( Msg.Noop ) )
