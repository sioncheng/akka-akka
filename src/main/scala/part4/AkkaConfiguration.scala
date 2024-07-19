package part4

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import akka.actor.typed.ActorSystem

object AkkaConfiguration {

  object SimpileLoggingActor {
    def apply(): Behavior[String] =
        Behaviors.receive { (ctx, msg) =>
        ctx.log.info(s"### message $msg")
        Behaviors.same
    }
  }

  def demoInlineConfig(): Unit = {
    val configString :String =
        """
        akka {
            loglevel = "INFO"
        }
        """
    val config = ConfigFactory.parseString(configString)
    val system = ActorSystem(SimpileLoggingActor(), "configDemo", ConfigFactory.load(config))

    system ! "A message to remember"

    Thread.sleep(1000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoInlineConfig()
  }
}
