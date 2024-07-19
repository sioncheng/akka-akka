package part5

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.util.Success
import scala.util.Failure
import akka.actor.typed.ActorSystem

object PipeDemo {
  val db: Map[String, Int] = Map(
    "Daniel" -> 241,
    "Jane" -> 345,
    "Dee" -> 424
  )

  val executor = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(executor)
  
  def callExternalService(name: String): Future[Int] = {
    Thread.sleep(50)
    Future(db(name))
  }

  sealed trait PhoneCallProtocol
  case class FindAndCallPhoneNumber(name: String)
    extends PhoneCallProtocol
  case class InitiatePhoneCall(number: Int)
    extends PhoneCallProtocol
  case class LogPhoneCallFailure(reason: Throwable)
    extends PhoneCallProtocol

  object PhoneCallActor {
    def apply(): Behavior[PhoneCallProtocol] = Behaviors.receive {(ctx, msg) =>
      msg match {
        case FindAndCallPhoneNumber(name) => 
            ctx.log.info(s"fetching the phone number for $name")
            val fetching = callExternalService(name)
            ctx.pipeToSelf(fetching) {
                case Success(value) => 
                    InitiatePhoneCall(value)
                case Failure(exception) => 
                    LogPhoneCallFailure(exception)
            }
            Behaviors.same
        case InitiatePhoneCall(number) => 
            ctx.log.info(s"initiating phone call to $number")
            Behaviors.same
        case LogPhoneCallFailure(reason) => 
            ctx.log.warn(s"initiating phone call failed: $reason")
            Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val guardian = Behaviors.setup[Unit] {ctx =>
      val phoneCallActor = ctx.spawn(PhoneCallActor(), "phoneCallActor")
      phoneCallActor ! FindAndCallPhoneNumber("Superman")
      Behaviors.same
    }

    val system = ActorSystem(guardian, "demoPipePattern")
    Thread.sleep(2000)
    system.terminate()
  }
}
