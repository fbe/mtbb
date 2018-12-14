package name.felixbecker.mtbb

import java.util.Date

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import name.felixbecker.mtbb.Telegram.NotifyTelegram
import io.circe.syntax._
import io.circe.syntax._
import io.circe.generic.auto._
import name.felixbecker.mtbb.JsonMessage.TelegramTextMessage

object JsonMessage {
  case class TelegramTextMessage(chat_id: Int, text: String, parse_mode: String = "HTML")
}

object Telegram {
  case class TelegramMessageOccurred(message: String)
  case class NotifyTelegram(message: String)

  def props = Props(classOf[Telegram])
}

class Telegram extends Actor {


  implicit val system = context.system
  implicit val ec = context.dispatcher

  def get(method: String) = {
    val callUri = s"https://api.telegram.org/bot${BotConfig.telegramApiKey}/$method"
    Http().singleRequest(HttpRequest(uri = callUri)).onComplete(println)
  }

  def post(method: String, payload: String): Unit = {
    val callUri = s"https://api.telegram.org/bot${BotConfig.telegramApiKey}/$method"
    Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = callUri, entity = HttpEntity(ContentTypes.`application/json`, payload))).onComplete(println)
  }

  get("getMe")
  get("getUpdates")
  post("sendMessage", s"""{"chat_id": -307322207, "text": "It's Wurst o clock ${new Date()}" }""")

  override def receive: Receive = {
    case NotifyTelegram(message: String) =>
      post("sendMessage", TelegramTextMessage(-307322207, message).asJson.toString())
  }
}
