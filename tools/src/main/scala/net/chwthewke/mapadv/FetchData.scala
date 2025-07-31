package net.chwthewke.mapadv

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp

object FetchData extends IOApp:
  override def run( args: List[String] ): IO[ExitCode] =
    FetchDepartments.fetchDepartments[IO]( Options() ).as( ExitCode.Success )
