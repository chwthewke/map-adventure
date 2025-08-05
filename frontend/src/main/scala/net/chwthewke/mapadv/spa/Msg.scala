package net.chwthewke.mapadv
package spa

enum Msg:
  case Noop
  case ReceiveMapData( data: MapData )
