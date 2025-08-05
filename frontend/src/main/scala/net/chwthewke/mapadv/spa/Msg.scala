package net.chwthewke.mapadv
package spa

import games.DistanceGame

enum Msg:
  case Noop
  case ReceiveMapData( data: MapData )
  case DistanceGameMsg( msg: DistanceGame.Msg )
