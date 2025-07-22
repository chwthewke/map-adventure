package net.chwthewke.mapadv
package spa
package css

import assets.css.BulmaClasses

trait Bulma extends BulmaClasses[CssClass]

object Bulma extends Bulma:
  override def cls( name: String ): CssClass = CssClass( "bulma-" + name )
