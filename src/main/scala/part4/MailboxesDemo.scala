package part4

import com.typesafe.config.Config
import akka.dispatch.UnboundedPriorityMailbox
import akka.dispatch.PriorityGenerator
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.MailboxSelector
import com.typesafe.config.ConfigFactory
import akka.actor.typed.ActorSystem


object MailboxesDemo {
  sealed trait Command
  case class SupportTicket(contents: String) extends Command
  case class Log(contents: String) extends Command

  object LoggerActor {
    def apply(): Behavior[Command] =
        Behaviors.receive[Command] {(ctx,msg) =>
            ctx.log.info(s"[loggerActor] received $msg")
            Behaviors.same
        }
  }

  class SupportTicketPriorityMailbox(settings: akka.actor.ActorSystem.Settings,
    config: Config) extends UnboundedPriorityMailbox (
        PriorityGenerator {
            case SupportTicket(contents) if contents.startsWith("[P0]") => 0
            case SupportTicket(contents) if contents.startsWith("[P1]") => 1
            case SupportTicket(contents) if contents.startsWith("[P2]") => 2
            case SupportTicket(contents) if contents.startsWith("[P3]") => 3
            case _ => 4
        }
    )

  def demoSupportTicketMailbox(): Unit = {
    val guardian = Behaviors.setup[Unit] {ctx =>
        val actor = ctx.spawn(LoggerActor(), "ticketLogger", MailboxSelector.fromConfig("support-ticket-mailbox"))

        actor ! Log("This is a log that is received first but processed last")
        actor ! SupportTicket("[P1] this thing is broken")
        actor ! SupportTicket("[P0] FIX THIS NOEW!")
        actor ! SupportTicket("[P3] something nice to have")

        Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoMailbox",
        ConfigFactory.load().getConfig("mailboxes-demo"))
    
    Thread.sleep(1000)

    system.terminate()
  }


  def main(args: Array[String]): Unit = {
    println("### MailboxesDemo")

    demoSupportTicketMailbox()
  }
}
