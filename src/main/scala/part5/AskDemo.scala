package part5

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Scheduler
import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.typed.delivery.WorkPullingProducerController
import scala.util.Success
import scala.util.Failure

object AskDemo {
  sealed trait WorkProtocol
  case class ComputationalTask(payload: String, reply: ActorRef[WorkProtocol])
    extends WorkProtocol
  case class ComputationalResult(result: Int)
    extends WorkProtocol

  object Worker {
    def apply(): Behavior[WorkProtocol] = Behaviors.receive { (ctx, msg) =>
      msg match {
        case ComputationalTask(payload, reply) => 
            ctx.log.info(s"[worker] crunching data for $payload")
            reply ! ComputationalResult(payload.split(" ").length)
            Behaviors.same
        case _ =>
            Behaviors.same
      }
    }
  }

  def askSimple(): Unit = {
    import akka.actor.typed.scaladsl.AskPattern._

    val system = ActorSystem(Worker(), "demoAskSimple")
    implicit val timeout: Timeout = Timeout(2.seconds)
    implicit val scheduler: Scheduler = system.scheduler

    val reply: Future[WorkProtocol] = 
        system.ask(ref => ComputationalTask("Trying the ask pattern", ref))
    
    implicit val ec: ExecutionContext = system.executionContext
    reply.foreach(println)

    Thread.sleep(2000)

    system.terminate()
  }

  def askFromWithinAnotherActor(): Unit = {
    val guardian = Behaviors.setup[WorkProtocol] {ctx =>
      val worker = ctx.spawn(Worker(), "worker")

      case class ExtendedComputationalResult(count: Int, desc: String)
       extends WorkProtocol
      
      implicit val timeout: Timeout = Timeout(2.seconds)

      ctx.ask(worker, ref => ComputationalTask("This ask pattern seems quite complicated", ref)) {
        case Success(ComputationalResult(count)) =>
          ExtendedComputationalResult(count, "ok")
        case Failure(ex) =>
          ExtendedComputationalResult(-1, s"failure: ${ex}")
        case x: Any =>
          ExtendedComputationalResult(-1, s"failure: ${x}")
      }
      

      Behaviors.receive[WorkProtocol] {(ctx, msg) =>
        msg match {
          case ExtendedComputationalResult(count, desc) =>
            ctx.log.info(s"asked and received: $desc -> $count")
            Behaviors.same
        case _ =>
            Behaviors.same
        }
      }
    }

    val system = ActorSystem(guardian, "askFromWithinAnotherActor")
    Thread.sleep(3000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    //askSimple()
    askFromWithinAnotherActor()
  }
}
