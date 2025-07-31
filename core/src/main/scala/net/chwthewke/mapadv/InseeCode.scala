package net.chwthewke.mapadv

import cats.Order
import cats.Show
import io.circe.Decoder
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder

opaque type InseeCode = String

object InseeCode:
  inline def apply( code: String ): InseeCode    = code
  extension ( code: InseeCode ) def code: String = code

  given Show[InseeCode]     = Show[String]
  given Order[InseeCode]    = Order[String]
  given Ordering[InseeCode] = Ordering[String]

  given Decoder[InseeCode] = Decoder[String]
  given Encoder[InseeCode] = Encoder[String]

  given KeyDecoder[InseeCode] = KeyDecoder[String]
  given KeyEncoder[InseeCode] = KeyEncoder[String]
