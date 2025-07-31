package net.chwthewke.mapadv

import cats.syntax.all.*
import io.circe.Decoder

case class ApiTownship(
    insee: InseeCode,
    name: String,
    department: DepartmentCode,
    center: ApiPoint,
    population: Int,
    contour: ApiContour
):
  def data: Township =
    Township( insee, name, department, center.point, population )

object ApiTownship:
  given Decoder[DepartmentCode => ApiTownship] = Decoder.instance: hc =>
    for
      name       <- hc.get[String]( "nom" )
      insee      <- hc.get[String]( "code" )
      center     <- hc.downField( "centre" ).get[ApiPoint]( "coordinates" )
      population <- hc.get[Option[Int]]( "population" )
      contour    <- hc.get[ApiContour]( "contour" )
    yield ApiTownship( InseeCode( insee ), name, _, center, population.orEmpty, contour )
