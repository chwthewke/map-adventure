package net.chwthewke.mapadv

import cats.Semigroup
import cats.Show
import cats.data.NonEmptyVector
import cats.derived.strict.*
import cats.syntax.all.*
import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder
import scala.collection.immutable.SortedMap

case class MapData(
    townships: SortedMap[InseeCode, Township],
    components: NonEmptyVector[Component],
    adjacencies: Map[InseeCode, NonEmptyVector[InseeCode]]
) derives ConfiguredDecoder,
      ConfiguredEncoder,
      Show

object MapData:
  def of(
      townships: SortedMap[InseeCode, Township],
      components: NonEmptyVector[NonEmptyVector[InseeCode]],
      adjacencies: Map[InseeCode, NonEmptyVector[InseeCode]]
  ): MapData =
    MapData(
      townships,
      components.map( Component( townships, _ ) ),
      adjacencies
    )

  given Semigroup[MapData]:
    override def combine( x: MapData, y: MapData ): MapData =
      MapData(
        x.townships ++ y.townships,
        x.components.concatNev( y.components ),
        x.adjacencies |+| y.adjacencies
      )
