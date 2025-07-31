package net.chwthewke.mapadv

import cats.Show
import io.circe.Decoder
import io.circe.Encoder
import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder

case class Point( lng: BigDecimal, lat: BigDecimal ) derives ConfiguredDecoder, ConfiguredEncoder

object Point:
  given Show[Point]:
    override def show( t: Point ): String = s"(${t.lng},${t.lat})"
