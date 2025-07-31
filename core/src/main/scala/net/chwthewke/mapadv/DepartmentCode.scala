package net.chwthewke.mapadv

import cats.Show
import io.circe.Decoder
import io.circe.Encoder

opaque type DepartmentCode = String

object DepartmentCode:
  inline def apply( code: String ): DepartmentCode    = code
  extension ( code: DepartmentCode ) def code: String = code

  given Show[DepartmentCode]    = Show[String]
  given Decoder[DepartmentCode] = Decoder[String]
  given Encoder[DepartmentCode] = Encoder[String]
