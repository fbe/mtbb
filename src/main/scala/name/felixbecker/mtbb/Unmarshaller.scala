package name.felixbecker.mtbb

import com.google.protobuf.GeneratedMessageV3
import name.felixbecker.mtbb.protobuf.MumbleProtos._

object Unmarshaller {

  def unmarshall(typeCode: Short, length: Int, protoBufBytes: Array[Byte]): GeneratedMessageV3 = typeCode match {
    case 0 => Version.parseFrom(protoBufBytes)
    case 1 => UDPTunnel.parseFrom(protoBufBytes)
    case 2 => Authenticate.parseFrom(protoBufBytes)
    case 3 => Ping.parseFrom(protoBufBytes)
    case 4 => Reject.parseFrom(protoBufBytes)
    case 5 => ServerSync.parseFrom(protoBufBytes)
    case 6 => ChannelRemove.parseFrom(protoBufBytes)
    case 7 => ChannelState.parseFrom(protoBufBytes)
    case 8 => UserRemove.parseFrom(protoBufBytes)
    case 9 => UserState.parseFrom(protoBufBytes)
    case 10 => BanList.parseFrom(protoBufBytes)
    case 11 => TextMessage.parseFrom(protoBufBytes)
    case 12 => PermissionDenied.parseFrom(protoBufBytes)
    case 13 => ACL.parseFrom(protoBufBytes)
    case 14 => QueryUsers.parseFrom(protoBufBytes)
    case 15 => CryptSetup.parseFrom(protoBufBytes)
    case 16 => ContextActionModify.parseFrom(protoBufBytes)
    case 17 => ContextAction.parseFrom(protoBufBytes)
    case 18 => UserList.parseFrom(protoBufBytes)
    case 19 => VoiceTarget.parseFrom(protoBufBytes)
    case 20 => PermissionQuery.parseFrom(protoBufBytes)
    case 21 => CodecVersion.parseFrom(protoBufBytes)
    case 22 => UserStats.parseFrom(protoBufBytes)
    case 23 => RequestBlob.parseFrom(protoBufBytes)
    case 24 => ServerConfig.parseFrom(protoBufBytes)
    case 25 => SuggestConfig.parseFrom(protoBufBytes)
  }

}