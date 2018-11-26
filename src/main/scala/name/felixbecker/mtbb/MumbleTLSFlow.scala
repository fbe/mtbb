package name.felixbecker.mtbb

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, TLSClientAuth, TLSProtocol}
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import javax.net.ssl.SSLContext

import scala.concurrent.Future

object MumbleTLSFlow {
  def apply(host: String, port: Int)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer): Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] = {
    val sslConfig = AkkaSSLConfig()
    val sslContext = SSLContext.getInstance("TLS")

    sslContext.init(null, Array(new InsecureSSLTrustManager), null)
    val defaultParams = sslContext.getDefaultSSLParameters
    val defaultProtocols = defaultParams.getProtocols
    val protocols = sslConfig.configureProtocols(defaultProtocols, sslConfig.config)
    defaultParams.setProtocols(protocols)

    val defaultCiphers = defaultParams.getCipherSuites
    val cipherSuites = sslConfig.configureCipherSuites(defaultCiphers, sslConfig.config)
    defaultParams.setCipherSuites(cipherSuites)

    val negotiateNewSession = TLSProtocol.negotiateNewSession
      .withCipherSuites(cipherSuites: _*)
      .withProtocols(protocols: _*)
      .withParameters(defaultParams)
      .withClientAuth(TLSClientAuth.None)


    val tlsConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] = Tcp()
      .outgoingTlsConnection(host, port, sslContext, negotiateNewSession)

    tlsConnection
  }
}
