package name.felixbecker.mtbb
import java.io.FileInputStream
import java.nio.file.Paths
import java.util.Properties
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

object BotConfig {

  val properties = new Properties()
  val propertiesFile = Paths.get(System.getProperty("user.home"), ".config", "mtbb.properties").toFile
  val fis = new FileInputStream(propertiesFile)
  properties.load(fis)
  fis.close()

  val telegramApiKey: String = properties.get("telegram.apikey").toString
  val systemName: String = System.getProperty("os.name")
  val systemVersion: String = System.getProperty("os.version")
  val userName: String = properties.get("mumble.botname").toString
  val hostname: String = properties.get("mumble.host").toString
  val port: Integer = Integer.valueOf(properties.get("mumble.port").toString)
  val keepAliveInterval: FiniteDuration = Duration.apply(properties.get("mumble.keepAliveIntervalSeconds").toString.toLong, TimeUnit.SECONDS)
}
