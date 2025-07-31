package net.chwthewke.mapadv

import cats.Show
import cats.derived.strict.*
import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder

case class Township(
    code: InseeCode,
    name: String,
    department: DepartmentCode,
    center: Point,
    population: Int
) derives ConfiguredDecoder,
      ConfiguredEncoder,
      Show
