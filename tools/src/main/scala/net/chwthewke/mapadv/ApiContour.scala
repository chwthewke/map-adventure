package net.chwthewke.mapadv

import cats.data.NonEmptyVector
import io.circe.Decoder
import io.circe.DecodingFailure

sealed trait ApiContour:
  def polygons: NonEmptyVector[ApiPolygon]
  def rings: NonEmptyVector[ApiLinearRing]

object ApiContour:
  implicit val apiContourDecoder: Decoder[ApiContour] = Decoder.instance[ApiContour]: hc =>
    hc.get[String]( "type" )
      .flatMap:
        case "Polygon"      => hc.get[ApiPolygon]( "coordinates" )
        case "MultiPolygon" => hc.get[ApiMultiPolygon]( "coordinates" )
        case t              => Left( DecodingFailure( s"Unknown contour type $t", hc.history ) )

case class ApiPolygon( exterior: ApiLinearRing, interiors: Vector[ApiLinearRing] ) extends ApiContour:
  override def polygons: NonEmptyVector[ApiPolygon] = NonEmptyVector.of( this )
  def rings: NonEmptyVector[ApiLinearRing]          = NonEmptyVector( exterior, interiors )

object ApiPolygon:
  implicit val apiPolygonDecoder: Decoder[ApiPolygon] =
    Decoder[NonEmptyVector[ApiLinearRing]].map( lrs => ApiPolygon( lrs.head, lrs.tail ) )

case class ApiMultiPolygon( override val polygons: NonEmptyVector[ApiPolygon] ) extends ApiContour:
  def rings: NonEmptyVector[ApiLinearRing] = polygons.flatMap( _.rings )

object ApiMultiPolygon:
  implicit val apiMultiPolygonDecoder: Decoder[ApiMultiPolygon] =
    Decoder[NonEmptyVector[ApiPolygon]].map( ApiMultiPolygon( _ ) )
