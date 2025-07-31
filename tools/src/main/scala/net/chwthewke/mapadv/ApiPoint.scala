package net.chwthewke.mapadv

import cats.Show
import io.circe.Decoder

opaque type ApiPoint = Point

object ApiPoint:
  inline def apply( lng: BigDecimal, lat: BigDecimal ): ApiPoint = Point( lng, lat )

  given pointDecoder: Decoder[ApiPoint] =
    Decoder[Vector[BigDecimal]].emap:
      case Vector( lng, lat ) => Right( Point( lng, lat ) )
      case v                  => Left( s"Invalid point (expected 2-element array, got ${v.size})." )

  given Show[ApiPoint] = Show[Point]

  extension ( point: ApiPoint )
    def point: Point    = point
    def lng: BigDecimal = point.lng
    def lat: BigDecimal = point.lat
