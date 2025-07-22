name := "map-adventure-build"

addSbtPlugin( "org.scoverage"       % "sbt-scoverage"            % "2.3.1" )
addSbtPlugin( "com.eed3si9n"        % "sbt-buildinfo"            % "0.13.1" )
addSbtPlugin( "com.github.sbt"      % "sbt-native-packager"      % "1.11.1" )
addSbtPlugin( "org.scalameta"       % "sbt-scalafmt"             % "2.5.5" )
addSbtPlugin( "org.jetbrains.scala" % "sbt-ide-settings"         % "1.1.3" )
addSbtPlugin( "org.scala-js"        % "sbt-scalajs"              % "1.19.0" )
addSbtPlugin( "org.portable-scala"  % "sbt-scalajs-crossproject" % "1.3.2" )
