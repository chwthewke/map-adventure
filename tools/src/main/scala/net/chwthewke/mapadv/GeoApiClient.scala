package net.chwthewke.mapadv

import cats.effect.Sync
import fs2.Stream
import org.http4s.MediaType
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.client.UnexpectedStatus
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Accept
import org.http4s.syntax.literals.*

class GeoApiClient[F[_]: Sync]( private val client: Client[F] ) extends Http4sClientDsl[F]:
  def getDepartment( code: DepartmentCode ): Stream[F, Byte] =
    val req: Request[F] = GET(
      ( uri"https://geo.api.gouv.fr/departements" / code.code / "communes" )
        .withQueryParam( "fields", "nom,code,centre,population,contour" )
        .withQueryParam( "format", "json" ),
      Accept( MediaType.application.json )
    )

    client
      .stream( req )
      .flatMap( resp =>
        if ( !resp.status.isSuccess )
          Stream.raiseError( UnexpectedStatus( resp.status, req.method, req.uri ) )
        else
          resp.body
      )

object GeoApiClient:
  def getDepartment[F[_]: Sync]( client: Client[F], code: DepartmentCode ): Stream[F, Byte] =
    new GeoApiClient[F]( client ).getDepartment( code )
