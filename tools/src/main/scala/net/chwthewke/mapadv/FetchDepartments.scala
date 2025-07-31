package net.chwthewke.mapadv

import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object FetchDepartments:

  private def writeToFile[F[_]: Async]( src: Stream[F, Byte], target: Path, useCache: Boolean )( using
      files: Files[F],
      console: Console[F]
  ): F[Unit] =
    OptionT
      .whenF( useCache )( files.isRegularFile( target ) )
      .getOrElse( false )
      .ifM(
        console.println( s"Skipped $target" ),
        src
          .through( Files[F].writeAll( target ) )
          .compile
          .drain <* console.println( s"Wrote $target" )
      )

  def fetchDepartment[F[_]: {Async, Files, Console}](
      client: Client[F],
      path: Path,
      useCache: Boolean,
      code: DepartmentCode
  ): F[Unit] =
    writeToFile( GeoApiClient.getDepartment[F]( client, code ), path / s"$code.json", useCache )

  def fetchDepartments[F[_]: Async]( options: Options ): F[Unit] =
    given Network[F]      = Network.forAsync
    given Console[F]      = Console.make
    given files: Files[F] = Files.forAsync

    files.createDirectories( options.targetDirectory ) *>
      EmberClientBuilder
        .default[F]
        .build
        .use( client =>
          Stream
            .emits( options.departmentCodes )
            .covary[F]
            .parEvalMapUnordered( 4 )( fetchDepartment( client, options.targetDirectory, options.useCache, _ ) )
            .compile
            .drain
        )
