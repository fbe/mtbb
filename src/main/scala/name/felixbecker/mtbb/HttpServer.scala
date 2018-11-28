package name.felixbecker.mtbb

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.circe.generic.auto._
import io.circe.syntax._
import name.felixbecker.mtbb.HttpServer.{Terminate, Terminated}
import name.felixbecker.mtbb.MumbleClient.{GetServerStatus, ServerStatus}

object HttpServer {

  case object Terminate
  case object Terminated

  def props(mumbleClient: ActorRef) = Props(classOf[HttpServer], mumbleClient)
}

class HttpServer(mumbleClient: ActorRef) extends Actor {

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()
  import context.dispatcher

  import scala.concurrent.duration._
  implicit val httpCallTimeout: Timeout = 1.second

  val route = get {
    pathEndOrSingleSlash {
      complete((mumbleClient ? GetServerStatus).mapTo[ServerStatus].map(x => x.asJson).map(_.toString))
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  override def receive: Receive = {

    case Terminate =>

      bindingFuture.map(_.unbind()).map(_ => self ! Terminated).recover { case _ => self ! Terminated }

      context.become {
        case Terminated => context.stop(self)
      }
  }


}