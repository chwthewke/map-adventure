package net.chwthewke.mapadv

import cats.syntax.all.*
import cats.data.NonEmptyVector
import fs2.Stream
import io.circe.Decoder

case class ApiLinearRing( points: NonEmptyVector[ApiPoint] ): // nominally >= 4 points with last == first
  def segments: Vector[( ApiPoint, ApiPoint )] =
    Stream
      .emits( points.toVector )
      .zipWithPrevious
      .mapFilter { case ( po, p ) => po.tupleRight( p ) }
      .toVector

object ApiLinearRing:
  implicit val linearRingDecoder: Decoder[ApiLinearRing] =
    Decoder[NonEmptyVector[ApiPoint]]
      .emap( nev =>
        if ( nev.length < 4 )
          Left( s"Too few points in linear ring (${nev.length})" )
        else if ( nev.head != nev.last )
          Left( s"Linear ring is not a loop (${nev.head} != ${nev.last})" )
        else Right( nev )
      )
      .map( ApiLinearRing( _ ) )
