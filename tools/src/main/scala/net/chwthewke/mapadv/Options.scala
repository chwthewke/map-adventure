package net.chwthewke.mapadv

import fs2.io.file.Path

case class Options(
    targetDirectory: Path = Path( "." ) / "tools" / "cache",
    useCache: Boolean = true,
    departmentCodes: Vector[DepartmentCode] = Options.defaultDepartmentCodes
)

object Options:
  val defaultDepartmentCodes: Vector[DepartmentCode] =
    (
      ( ( 1 to 19 ) ++ ( 21 to 95 ) ).map( n => f"$n%02d" ) ++
        Seq( "2A", "2B" ) ++
        ( ( 971 to 974 ) :+ 976 ).map( _.toString )
    )
      .map( DepartmentCode( _ ) )
      .to( Vector )
