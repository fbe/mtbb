package name.felixbecker.mtbb

import java.nio.{ByteBuffer, ByteOrder}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import name.felixbecker.mtbb.protobuf.MumbleProtos.{Authenticate, Ping, Version}

object MainStream extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("MTBB")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val tlsConnection = MumbleTLSFlow(BotConfig.hostname, BotConfig.port)

  val clientVersion = ByteString(Marshaller.marshall(Version.newBuilder().setRelease("""https://www.youtube.com/watch?v=Iew2KfocTcE (MTBB)""").setOs(BotConfig.systemName).setOsVersion(BotConfig.systemVersion).setVersion(1).build()))
  val authenticate = ByteString(Marshaller.marshall(Authenticate.newBuilder().setUsername(BotConfig.userName).build()))

  val actorTLSConnectionInput = Source.actorRef[ByteString](100, OverflowStrategy.fail)

  def computeFrameLength(offsetBytes: Array[Byte], computedSize: Int): Int = {
    println(s"Type ${ByteBuffer.wrap(offsetBytes).getShort()}, computed size: $computedSize ")
    computedSize + 6
  }

  val frameStage: Flow[ByteString, ByteString, NotUsed] = Framing.lengthField(
    fieldLength = 4,
    fieldOffset = 2,
    maximumFrameLength = 10000000,
    ByteOrder.BIG_ENDIAN,
    computeFrameLength)

  val extractorSink = Sink.foreach[ByteString] { bs =>
    try {
      val bb = bs.asByteBuffer
      val typeCode = bb.getShort() // 2 byte
      val typeLength = bb.getInt()

      println(s"Type length: $typeLength")
      println(s"All bytes received length: ${bs.length}")

      val typeBytes = new Array[Byte](typeLength)
      bb.get(typeBytes)

      if (typeCode != 1) {
        println(Unmarshaller.unmarshall(typeCode, typeLength, typeBytes))
      } else {
        println(s"Typecode 1 - typelengh $typeLength - ignoring")
      }
    } catch {
      case t: Throwable => t.printStackTrace()
    }

  }

  val mumbleOutgoing: ActorRef = actorTLSConnectionInput.via(tlsConnection).via(frameStage).to(extractorSink).run()

  def ping = ByteString(Marshaller.marshall(Ping.newBuilder().setTimestamp(System.currentTimeMillis()).build()))

  mumbleOutgoing ! clientVersion
  mumbleOutgoing ! authenticate
  import actorSystem.dispatcher

  import scala.concurrent.duration._
  actorSystem.scheduler.schedule(5.second, 5.second, mumbleOutgoing, ping)

}
