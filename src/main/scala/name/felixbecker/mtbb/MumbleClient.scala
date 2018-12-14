package name.felixbecker.mtbb

import akka.actor.{Actor, ActorRef, Props}
import name.felixbecker.mtbb.MumbleClient._
import name.felixbecker.mtbb.protobuf.MumbleProtos.{ChannelRemove, _}

object MumbleClient {
  def props(telegram: ActorRef) = Props(classOf[MumbleClient], telegram)

  case class FlowStarted(mumbleServer: ActorRef)

  case object GetServerStatus

  case class ServerStatus(lastPingReceivedTS: Option[Long] = None, channel: List[Channel] = Nil, users: List[User] = Nil)

  case class Channel(
                      channelId: Int,
                      parent: Option[Int] = None,
                      name: Option[String] = None,
                      links: List[Int] = Nil,
                      description: Option[String] = None,
                      temporary: Boolean = false,
                      position: Int = 0,
                      maxUsers: Option[Int] = None
                    )

  case class User(
                   sessionId: Int,
                   actor: Option[Int] = None,
                   name: Option[String] = None,
                   userId: Option[Int] = None,
                   channelId: Option[Int] = None,
                   mute: Option[Boolean] = None,
                   deaf: Option[Boolean] = None,
                   suppress: Option[Boolean] = None,
                   selfMute: Option[Boolean] = None,
                   selfDeaf: Option[Boolean] = None,
                   texture: Option[Array[Byte]] = None,
                   comment: Option[String] = None,
                   prioritySpeaker: Option[Boolean] = None,
                   recording: Option[Boolean] = None
                 )

}

class MumbleClient(telegram: ActorRef) extends Actor {

  val clientVersion = Version.newBuilder()
    .setRelease("""https://www.youtube.com/watch?v=Iew2KfocTcE (MTBB)""")
    .setOs(BotConfig.systemName)
    .setOsVersion(BotConfig.systemVersion)
    .setVersion(1)
    .build()

  val authenticate = Authenticate.newBuilder()
    .setUsername(BotConfig.userName)
    .build()

  var mumbleServer: Option[ActorRef] = None
  var serverStatus: ServerStatus = ServerStatus()
  def userName: Option[String] = serverStatus.users.find(_.sessionId == sessionId.get).map(_.name).head

  /* both assigned after server sync */
  var sessionId: Option[Int] = None

  override def receive: Receive = {

    case GetServerStatus =>
      sender() ! serverStatus

    case FlowStarted(mumbleServerRef) =>
      mumbleServer = Some(mumbleServerRef)
      mumbleServer.get ! clientVersion
      mumbleServer.get ! authenticate

    case cr: ChannelRemove =>
      serverStatus = serverStatus.copy(
        channel = serverStatus.channel.filterNot(_.channelId == cr.getChannelId)
      )

    case p: Ping =>
      serverStatus = serverStatus.copy(lastPingReceivedTS = Some(System.currentTimeMillis()))

    case cs: ChannelState => onChannelState(cs)
    case us: UserState => onUserState(us)

    case ss: ServerSync =>
      sessionId = Some(ss.getSession)
      val weltraum = serverStatus.channel.find(_.name.contains("weltraum")).get.channelId
      mumbleServer.get ! UserState.newBuilder().setSession(sessionId.get).setActor(sessionId.get).setChannelId(weltraum).build()

    case cs: CryptSetup =>
      cs.getClientNonce
      cs.getKey
      cs.getServerNonce
      // TODO do something with that

    case tm: TextMessage =>
      serverStatus.users.find(_.sessionId == tm.getActor).foreach { user =>
        user.name.foreach { userName =>
          telegram ! Telegram.NotifyTelegram(s"<b>${userName}</b>: ${tm.getMessage}")
        }
      }
    case unknown =>
      println(s"Received unknown message ${unknown.getClass}")
  }

  def onChannelState(cs: ChannelState): Unit = {
    val channelId = if (cs.hasChannelId) Some(cs.getChannelId) else None
    channelId match {

      case Some(cid) =>

        val oldChannel = serverStatus.channel.find(_.channelId == cid)
        oldChannel.foreach { oc =>
          serverStatus = serverStatus.copy(channel = serverStatus.channel.filterNot(_.equals(oc)))
        }

        import scala.collection.JavaConverters._
        val parent = if (cs.hasParent) Some(cs.getParent) else None
        val name = if (cs.hasName) Some(cs.getName) else None
        val links: Option[List[Int]] = if (cs.getLinksList.isEmpty) None else Some(cs.getLinksList.asScala.toList.map(i => i.toInt))
        val description = if (cs.hasDescription) Some(cs.getDescription) else None
        val temporary = if (cs.hasTemporary) Some(cs.getTemporary) else None
        val position = if (cs.hasPosition) Some(cs.getPosition) else None
        val maxUsers = if (cs.hasMaxUsers) Some(cs.getMaxUsers) else None

        val ocCopy = oldChannel.getOrElse(Channel(cid))
        val newChannel = ocCopy.copy(
          parent = parent.orElse(ocCopy.parent),
          name = name.orElse(ocCopy.name),
          links = links.getOrElse(ocCopy.links),
          description = description.orElse(ocCopy.description),
          temporary = temporary.getOrElse(ocCopy.temporary),
          position = position.getOrElse(ocCopy.position),
          maxUsers = maxUsers.orElse(ocCopy.maxUsers)
        )

        serverStatus = serverStatus.copy(channel = newChannel :: serverStatus.channel)

      case None =>
        println(s"ignoring channel state message $cs because channelId is missing")
    }

  }

  def opt[T](exists: Boolean, provide: => T): Option[T] = {
    if(exists) Some(provide) else None
  }

  def onUserState(us: UserState): Unit = {

    val session = if (us.hasSession) Some(us.getSession) else None
    session match {
      case Some(sid) =>

        val oldUser = serverStatus.users.find(_.sessionId == sid)
        oldUser.foreach { ou =>
          serverStatus = serverStatus.copy(users = serverStatus.users.filterNot(_.equals(ou)))
        }

        val actor = opt(us.hasActor, us.getActor)
        val name = opt(us.hasName, us.getName)
        val userId = opt(us.hasUserId, us.getUserId)
        val channelId = opt(us.hasChannelId, us.getChannelId)
        val mute = opt(us.hasMute, us.getMute)
        val deaf = opt(us.hasDeaf, us.getDeaf)
        val suppress = opt(us.hasSuppress, us.getSuppress)
        val selfMute = opt(us.hasSelfMute, us.getSelfMute)
        val selfDeaf = opt(us.hasSelfDeaf, us.getSelfDeaf)
        val texture = opt(us.hasTexture, us.getTexture).map(_.toByteArray)
        val comment = opt(us.hasComment, us.getComment)
        val prioritySpeaker = opt(us.hasPrioritySpeaker, us.getPrioritySpeaker)
        val recording = opt(us.hasRecording, us.getRecording)


        val ouCopy = oldUser.getOrElse(User(sid))

        val newUser = ouCopy.copy(
          actor = actor.orElse(ouCopy.actor),
          name = name.orElse(ouCopy.name),
          userId = userId.orElse(ouCopy.userId),
          channelId = channelId.orElse(ouCopy.channelId),
          mute = mute.orElse(ouCopy.mute),
          deaf = deaf.orElse(ouCopy.deaf),
          suppress = suppress.orElse(ouCopy.suppress),
          selfMute = selfMute.orElse(ouCopy.selfMute),
          selfDeaf = selfDeaf.orElse(ouCopy.selfDeaf),
          texture = texture.orElse(ouCopy.texture),
          comment = comment.orElse(ouCopy.comment),
          prioritySpeaker = prioritySpeaker.orElse(ouCopy.prioritySpeaker),
          recording = recording.orElse(ouCopy.recording)
        )

        serverStatus = serverStatus.copy(users = newUser :: serverStatus.users)

      case None =>
        println(s"ignoring user state message $us because session is missing")
    }

  }

}