package net.chwthewke.mapadv

import tyrian.Attr
import tyrian.Elem
import tyrian.Empty
import tyrian.EmptyAttribute

package object spa:
  given convertAttrOption[A]: Conversion[Option[Attr[A]], Attr[A]] = _.fold[Attr[A]]( EmptyAttribute )( identity )

  given convertElemOption[A]: Conversion[Option[Elem[A]], Elem[A]] = _.fold[Elem[A]]( Empty )( identity )
