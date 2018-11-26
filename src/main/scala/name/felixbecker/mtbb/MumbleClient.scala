package name.felixbecker.mtbb

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import name.felixbecker.mtbb.MainStream.mumbleServer
import name.felixbecker.mtbb.MumbleClient.FlowStarted
import name.felixbecker.mtbb.protobuf.MumbleProtos.{Authenticate, Version}

object MumbleClient {
  def props() = Props(classOf[MumbleClient])
  case class FlowStarted(mumbleServer: ActorRef)
}

class MumbleClient() extends Actor {

  val clientVersion = Version.newBuilder()
    .setRelease("""https://www.youtube.com/watch?v=Iew2KfocTcE (MTBB)""")
    .setOs(BotConfig.systemName)
    .setOsVersion(BotConfig.systemVersion)
    .setVersion(1)
    .build()

  val authenticate = Authenticate.newBuilder()
    .setUsername(BotConfig.userName)
    .build()

  private var mumbleServer: Option[ActorRef] = None

  override def receive: Receive = {
    case FlowStarted(mumbleServerRef) =>
      mumbleServer = Some(mumbleServerRef)
      mumbleServer.get ! clientVersion
      mumbleServer.get ! authenticate
    case unknown =>
      println(s"Received unknown message $unknown")
  }
}