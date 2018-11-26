package name.felixbecker.mtbb

import java.nio.{ByteBuffer, ByteOrder}

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import com.google.protobuf.GeneratedMessageV3
import name.felixbecker.mtbb.MumbleClient.FlowStarted
import name.felixbecker.mtbb.protobuf.MumbleProtos.{Authenticate, Ping, QueryUsers, Version}

object MainStream extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("MTBB")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val tlsConnection = MumbleTLSFlow(BotConfig.hostname, BotConfig.port)



  val mumbleServerSource = Source.actorRef[GeneratedMessageV3](100, OverflowStrategy.fail)

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

  val byteStringToProtoBufGeneratedMessage: Flow[ByteString, GeneratedMessageV3, NotUsed] = Flow[ByteString].map { bs =>
    val bb = bs.asByteBuffer
    val typeCode = bb.getShort() // 2 byte
    val typeLength = bb.getInt()
    val typeBytes = new Array[Byte](typeLength)
    bb.get(typeBytes)
    Unmarshaller.unmarshall(typeCode, typeLength, typeBytes)
  }

  val mumbleClient = actorSystem.actorOf(MumbleClient.props())
  val mumbleClientSink = Sink.actorRef[GeneratedMessageV3](mumbleClient, Done)

  val keepAlive: Flow[GeneratedMessageV3, GeneratedMessageV3, NotUsed] = Flow[GeneratedMessageV3].keepAlive(BotConfig.keepAliveInterval, () => createPingPackage)

  /* Defined in mumble.proto: UDPTunnel, not used. Not even for tunneling UDP through TCP */
  def isUDPTunnelPackage(rawByteString: ByteString): Boolean = {
    rawByteString.asByteBuffer.getShort() == 1
  }

  def createPingPackage = Ping.newBuilder().setTimestamp(System.currentTimeMillis()).build()

  val marshallProtobufMessageToByteString: Flow[GeneratedMessageV3, ByteString, NotUsed] = Flow[GeneratedMessageV3].map { m =>
    ByteString(Marshaller.marshall(m))
  }

  val mumbleServer: ActorRef = mumbleServerSource
    .via(keepAlive) // https://github.com/mumble-voip/mumble/issues/3550
    .via(marshallProtobufMessageToByteString)
    .via(tlsConnection)
    .via(frameStage)
    .filterNot(isUDPTunnelPackage)
    .via(byteStringToProtoBufGeneratedMessage)
    .log("mumbleconnectionflow")
    .to(mumbleClientSink)
    .run()


  mumbleClient ! FlowStarted(mumbleServer)




}

