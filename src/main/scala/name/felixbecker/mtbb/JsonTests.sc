import io.circe.syntax._
import io.circe.generic.auto._

case class TelegramTextMessage(chat_id: Int, text: String)

println(TelegramTextMessage(4711, "foobar").asJson)