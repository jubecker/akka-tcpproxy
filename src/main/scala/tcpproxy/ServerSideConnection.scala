package tcpproxy

import akka.actor._
import akka.event.LoggingReceive
import akka.io.{IO, Tcp}

object ServerSideConnection {
  object Ack extends Tcp.Event

  def props(config: TCPProxyConfig) =
    Props(classOf[ServerSideConnection],config)

}

class ServerSideConnection(config: TCPProxyConfig) extends Actor with Stash with ActorLogging {

  import TCPProxy._

  IO(Tcp)(context.system) ! Tcp.Connect(config.remoteEndpoint)

  def receive = LoggingReceive {
    case Tcp.Connected(remote, local) =>
      log.info("connected to {}", remote)
      sender ! Tcp.Register(self)
      unstashAll()
      context become connected(sender)

    case Tcp.CommandFailed(cmd) =>
      log.error("command failed {}", cmd)
      context stop self

    case ClientConnectionClosed =>
      context stop self

    case _ => stash()
  }

  def connected(connection: ActorRef): Receive = {
    case Tcp.Received(data) =>
      context.parent ! ServerData(data)

    case ClientData(data) =>
      connection ! Tcp.Write(data, ServerSideConnection.Ack)
      context.become(waitingForAck, discardOld = false)

    case Tcp.CommandFailed(cmd) =>
      log.error("command failed {}", cmd)
      context stop self

    case ClientConnectionClosed =>
      connection ! Tcp.Close

    case _:Tcp.ConnectionClosed =>
      context.parent ! ServerConnectionClosed
      context stop self
  }

  def waitingForAck: Receive = {
    case ServerSideConnection.Ack =>
      unstashAll()
      context unbecome()

    case _ => stash()
  }

}


