package net.chwthewke.mapadv
package spa
package css

import tyrian.Attr
import tyrian.Html

case class Classes( classes: Vector[CssClass] ):
  def +( bc: CssClass ): Classes          = copy( classes = classes :+ bc )
  def +:( bc: CssClass ): Classes         = copy( classes = bc +: classes )
  def +( bc: Classes ): Classes           = copy( classes = classes ++ bc.classes )
  def +( bc: Option[CssClass] ): Classes  = copy( classes = classes ++ bc )
  def +:( bc: Option[CssClass] ): Classes = copy( classes = bc ++: classes )

  def attr: Attr[Nothing] =
    Html.className := classes.distinct.mkString( " " )

object Classes:
  def apply( classes: CssClass* ): Classes = Classes( classes.toVector )

  given Conversion[Classes, Attr[Nothing]] = cs => cs.attr
