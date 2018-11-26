package name.felixbecker.mtbb

object ServerModel {
  case class ServerState(channels: String)
}

object DeltaEvents {
  case class ChannelCreated()
  case class ChannelDeleted()
  case class UserJoinedServer()
  case class UserLeftServer()
  case class UserJoinedChannel()

}
