package net.chwthewke.mapadv
package spa.css

import tyrian.Attr
import tyrian.EmptyAttribute

opaque type CssClass = String

object CssClass:
  inline def apply( name: String ): CssClass = name

  extension ( cc: CssClass )
    def `class`: String                   = cc
    def +( c: CssClass ): Classes         = Classes( Vector( cc, c ) )
    def +( c: Option[CssClass] ): Classes = Classes( Vector( cc ) ++ c )

  extension ( cc: Option[CssClass] )
    def +( c: CssClass ): Classes         = Classes( cc ++: Vector( c ) )
    def +( c: Option[CssClass] ): Classes = Classes( cc.toVector ++ c )

  given Conversion[CssClass, Classes]               = c => Classes( c )
  given Conversion[Option[CssClass], Attr[Nothing]] = c => c.fold( EmptyAttribute )( c => Classes( c ).attr )
  given Conversion[CssClass, Attr[Nothing]]         = c => Classes( c ).attr
