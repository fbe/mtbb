package name.felixbecker.mtbb
import java.security.SecureRandom

import scala.concurrent.duration._

object BotConfig {
  val systemName: String = System.getProperty("os.name")
  val systemVersion: String = System.getProperty("os.version")
  val userName = "MTBB"
  val hostname = "mumble.coding4.coffee"
  val port = 64738
  val keepAliveInterval: FiniteDuration = 15.seconds
}
