package net.chwthewke.mapadv

import cats.Show
import cats.data.NonEmptyVector
import cats.derived.strict.*
import cats.syntax.all.*
import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder

case class Component(
    townships: NonEmptyVector[InseeCode],
    maxPopulation: Int
) derives ConfiguredDecoder,
      ConfiguredEncoder,
      Show

object Component:
  def apply( townships: Map[InseeCode, Township], contents: NonEmptyVector[InseeCode] ): Component =
    Component( contents, contents.toVector.mapFilter( townships.get ).map( _.population ).maximumOption.orEmpty )
