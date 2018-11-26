package name.felixbecker.mtbb
import java.nio.ByteBuffer

import com.google.protobuf.GeneratedMessageV3
import name.felixbecker.mtbb.protobuf.MumbleProtos._

object Marshaller {

  def marshall(message: GeneratedMessageV3): Array[Byte] = {

    val messageBytes = message.toByteArray
    val typeCode = getTypeCodeForGeneratedMessage(message)
    val typeLength = messageBytes.length

    val buffer = ByteBuffer.allocate(messageBytes.length + 6)
    buffer.putShort(typeCode)
    buffer.putInt(typeLength)
    buffer.put(messageBytes)

    buffer.array()
  }


  def getTypeCodeForGeneratedMessage(generatedMessage: GeneratedMessageV3): Short = generatedMessage match {
    case _: Version => 0
    case _: UDPTunnel => 1
    case _: Authenticate => 2
    case _: Ping => 3
    case _: Reject => 4
    case _: ServerSync => 5
    case _: ChannelRemove => 6
    case _: ChannelState => 7
    case _: UserRemove => 8
    case _: UserState => 9
    case _: BanList => 10
    case _: TextMessage => 11
    case _: PermissionDenied => 12
    case _: ACL => 13
    case _: QueryUsers => 14
    case _: CryptSetup => 15
    case _: ContextActionModify => 16
    case _: ContextAction => 17
    case _: UserList => 18
    case _: VoiceTarget => 19
    case _: PermissionQuery => 20
    case _: CodecVersion => 21
    case _: UserStats => 22
    case _: RequestBlob => 23
    case _: ServerConfig => 24
    case _: SuggestConfig => 25
  }
}
