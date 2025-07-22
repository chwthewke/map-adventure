import java.time.Instant
import java.time.temporal.ChronoUnit
import sbt._
import sbt.Keys._
import sbtbuildinfo._
import sbtbuildinfo.BuildInfoKeys._

object BuildInfo extends AutoPlugin {

  override def requires: Plugins = super.requires && BuildInfoPlugin

  val shortVersion = SettingKey[String]( "short-version" ).withRank( KeyRanks.Invisible )

  val MajorMinor = raw"(\d+)\.(\d+).*".r

  private def shortenVersion( version: String ): String =
    version match {
      case MajorMinor( maj, min ) => s"$maj.$min"
      case _                      => throw new IllegalArgumentException( s"Could not parse version $version" )
    }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    buildInfoObject := "MapAdventure",
    buildInfoPackage := "buildinfo",
    shortVersion := shortenVersion( version.value ),
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      BuildInfoKey.action( "builtAt" )( Instant.now().truncatedTo( ChronoUnit.SECONDS ) )
    )
  )
}
