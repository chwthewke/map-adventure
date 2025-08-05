package net.chwthewke.mapadv

import math.*
import scala.annotation.tailrec

object Distance:
  // all of this: https://en.wikipedia.org/wiki/Vincenty%27s_formulae
  object WGS84:
    val a: Double = 6378137.0
    val f: Double = 1d / 298.257223563
    val b: Double = 6356752.314245

  def apply( p: Point, q: Point ): Double =
    val U_1: Double = reducedLatitude( p.lat.toDouble )
    val U_2: Double = reducedLatitude( q.lat.toDouble )
    val L           = degreesToRadians( q.lng.toDouble - p.lng.toDouble )
    loop( L, U_1, U_2, L, 0 )

  def degreesToRadians( d: Double ): Double = Pi * d / 180d

  def reducedLatitude( lat: Double ): Double =
    val Φ: Double = degreesToRadians( lat )
    atan( ( 1 - WGS84.f ) * tan( Φ ) )

  @tailrec
  def loop( λ: Double, U_1: Double, U_2: Double, L: Double, N: Int ): Double =
    step( λ, U_1, U_2, L ) match
      case Left( next ) if N < 1000 => loop( next, U_1, U_2, L, N + 1 )
      case Left( approx )           => approx
      case Right( value )           => value

  def step( λ: Double, U_1: Double, U_2: Double, L: Double ): Either[Double, Double] =
    val sin_σ: Double = sqrt:
      pow( cos( U_2 ) * sin( λ ), 2 ) + pow( cos( U_1 ) * sin( U_2 ) - sin( U_1 ) * cos( U_2 ) * cos( λ ), 2 )
    val cos_σ: Double    = sin( U_1 ) * sin( U_2 ) + cos( U_1 ) * cos( U_2 ) * cos( λ )
    val σ: Double        = atan2( sin_σ, cos_σ )
    val sin_α: Double    = cos( U_1 ) * cos( U_2 ) * sin( λ ) / sin_σ
    val sin2_α: Double   = pow( sin_α, 2 )
    val cos2_α: Double   = 1 - sin2_α
    val cos_2σm: Double  = cos_σ - 2 * sin( U_1 ) * sin( U_2 ) / cos2_α
    val C: Double        = WGS84.f / 16 * cos2_α * ( 4 + WGS84.f * ( 4 - 3 * cos2_α ) )
    val cos2_2σm: Double = pow( cos_2σm, 2 )
    val next_λ: Double   = L + ( 1 - C ) * WGS84.f * sin_α * (
      σ + C * sin_σ * ( cos_2σm + C * cos_σ * ( -1 + 2 * cos2_2σm ) )
    )
    if ( λ.abs > 0 )
      val dλ = ( ( λ - next_λ ) / λ ).abs
      println( s"λ: $λ -> $next_λ - Δ = $dλ" )
    if ( λ.abs < 1e-18 || ( ( λ - next_λ ) / λ ).abs < 1e-9 )
      val b2: Double = pow( WGS84.b, 2 )
      val u2: Double = cos2_α * ( ( pow( WGS84.a, 2 ) - b2 ) / b2 )
      val A: Double  = 1 + u2 / 16384 * ( 4096 + u2 * ( -768 + u2 * ( 320 - 175 * u2 ) ) )
      val B: Double  = u2 / 1024 * ( 256 + u2 * ( -128 + u2 * ( 74 - 47 * u2 ) ) )
      val Δσ: Double = B * sin_σ * (
        cos_2σm + B / 4 * ( cos_σ * ( -1 + 2 * cos2_2σm ) -
          B / 6 * cos_2σm * ( -3 + 4 * pow( sin_σ, 2 ) ) * ( -3 + 4 * cos2_2σm ) )
      )
      val s: Double = WGS84.b * A * ( σ - Δσ )
      Right( s )
    else Left( next_λ )
