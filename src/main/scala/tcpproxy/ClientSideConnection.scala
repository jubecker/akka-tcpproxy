package tcpproxy

import akka.actor._
import akka.io.Tcp

object ClientSideConnection {
  object Ack extends Tcp.Event

  def props(connection: ActorRef, config: TCPProxyConfig, serverSideProps: Props) =
    Props(new ClientSideConnection(connection, config, serverSideProps))
}

class ClientSideConnection(connection: ActorRef, config: TCPProxyConfig, serverSideProps: Props) extends Actor with Stash with ActorLogging {

  import TCPProxy._

  val serverConnection = context.actorOf(serverSideProps, "serverSide")
  context watch serverConnection

  def receive = {
    case Tcp.Received(data) =>
      serverConnection ! ClientData(data)

    case ServerData(data) =>
      connection ! Tcp.Write(data, ClientSideConnection.Ack)
      context.become(waitingForAck, discardOld = false)

    case Tcp.CommandFailed(cmd: Tcp.Write) =>
      log.warning("write failed")
      connection ! Tcp.ResumeWriting

    case Tcp.CommandFailed(cmd) =>
      log.error("command failed {}", cmd)
      serverConnection ! ClientConnectionClosed

    case ServerConnectionClosed =>
      connection ! Tcp.ConfirmedClose

    case _:Tcp.ConnectionClosed =>
      serverConnection ! ClientConnectionClosed
      context become disconnected

    case Terminated(serverConnection) =>
      connection ! Tcp.Close
      context stop self
  }

  def disconnected: Receive = {
    case Terminated(serverConnection) =>
      context stop self
    case ServerConnectionClosed =>
  }

  def waitingForAck: Receive = {
    case ClientSideConnection.Ack =>
      unstashAll()
      context unbecome()

    case _ => stash()
  }

}