package net.chwthewke.mapadv
package server

import cats.effect.Async
import cats.effect.Deferred
import cats.effect.Resource
import cats.syntax.all.*
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*
import scala.concurrent.duration.*

object AppServer:
  def resource[F[_]: Async]: Resource[F, Unit] =
    given Network[F] = Network.forAsync[F]

    for
      config   <- Resource.eval( ConfigSource.default.loadF[F, AppConfig]() )
      shutdown <- Resource.eval( Deferred[F, Unit] )
      _        <- EmberServerBuilder
             .default[F]
             .withHost( config.server.listenAddress )
             .withPort( config.server.listenPort )
             .withHttpApp( Routes( config.server, shutdown.complete( () ).void ).orNotFound )
             .withShutdownTimeout( 1.second )
             .build
      server <- Resource.make( shutdown.get )( _ => Async[F].unit )
    yield server
