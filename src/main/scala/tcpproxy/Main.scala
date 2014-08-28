package tcpproxy

import akka.actor.ActorSystem

object Main {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("usage: <local port>:<remote host>:<remote port> ...")
      System.exit(1)
    }

    val as = ActorSystem("tcpproxy")
    val configs = TCPProxyConfig.parse(args)
    configs foreach { config =>
      as.actorOf(TCPProxy.props(config), "tcpproxy-" + config.listenEndpoint.getPort())
    }
  }

}