package tcpproxy

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.ByteString

object TCPProxyConfig {
  def parse(args: Array[String]): Array[TCPProxyConfig] = args map parse

  def parse(arg: String) = {
    val parts = arg split ':'
    val listenPort = parts(0).toInt
    val remoteHost = parts(1)
    val remotePort = parts(2).toInt
    TCPProxyConfig(
      new InetSocketAddress(listenPort),
      new InetSocketAddress(remoteHost, remotePort)
    )
  }
}

case class TCPProxyConfig(
  listenEndpoint: InetSocketAddress,
  remoteEndpoint: InetSocketAddress
)


object TCPProxy {

  case class ServerData(data: ByteString)
  case object ServerConnectionClosed
  case class ClientData(data: ByteString)
  case object ClientConnectionClosed

  def props(config: TCPProxyConfig) = {
    Props(new TCPProxy(config))
  }
}


class TCPProxy(config: TCPProxyConfig) extends Actor with ActorLogging {

  IO(Tcp)(context.system) ! Tcp.Bind(self, config.listenEndpoint)

  def receive = {
    case Tcp.Bound(local) =>
      log.info("listening on {}", local)
      context become bound

    case Tcp.CommandFailed(cmd) =>
      log.error("error binding to socket")
      context stop self
  }

  def bound: Receive = {
    case Tcp.Connected(remote, local) =>
      log.info("connection from {}", remote)
      sender ! Tcp.Register(createConnectionHandler(sender, remote))

    case Tcp.CommandFailed(cmd) =>
      log.error("command failed {}", cmd)
      context stop self
  }

  def createConnectionHandler(connection: ActorRef, remote: InetSocketAddress) = {

    val serverSideProps = ServerSideConnection.props(config)
    context.actorOf(ClientSideConnection.props(connection, config, serverSideProps), "clientside-" + remote.getPort)
  }

}








