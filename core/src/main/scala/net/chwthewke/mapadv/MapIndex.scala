package net.chwthewke.mapadv

import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder

case class MapIndex( parts: Int, components: Int ) derives ConfiguredDecoder, ConfiguredEncoder
