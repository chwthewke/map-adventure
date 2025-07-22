package net.chwthewke.mapadv
package server

import cats.effect.Sync
import cats.syntax.all.*
import org.http4s.Charset
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.StaticFile
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.scalatags.*

import server.pages.Index

class Routes[F[_]: Sync](
    private val serverConfig: ServerConfig,
    private val shutdown: F[Unit]
) extends Http4sDsl[F]:

  private val systemRoutes: HttpRoutes[F] = HttpRoutes.of:
    case GET -> Root / "shutdown" => shutdown *> Ok()

  private val staticFileTypes: List[String] =
    List( ".js", ".css", ".map", ".png", ".ico", ".svg", ".json", ".ttf", ".woff", ".woff2" )

  private val staticRoutes: HttpRoutes[F] = HttpRoutes.of:
    case req @ GET -> "static" /: rest if staticFileTypes.exists( rest.renderString.endsWith ) =>
      StaticFile.fromResource[F]( "/" + rest.renderString, Some( req ) ).getOrElseF( NotFound() )

  private val pageRoutes: HttpRoutes[F] = HttpRoutes.of:
    case GET -> Root / "index.html" =>
      Ok( Index.page )
    case GET -> Root / "js" / "launcher.js" =>
      Ok(
        Index.launcherScript( serverConfig.frontendFlags ),
        `Content-Type`( MediaType.application.javascript, Charset.`UTF-8` )
      )

  val routes: HttpRoutes[F] =
    systemRoutes <+> pageRoutes <+> staticRoutes

object Routes:
  def apply[F[_]: Sync](
      serverConfig: ServerConfig,
      shutdown: F[Unit]
  ): HttpRoutes[F] =
    new Routes(
      serverConfig,
      shutdown
    ).routes
