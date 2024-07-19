package part3

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef

import scala.concurrent.duration._

class EssentialTestingSpec 
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
    
    import TestingActors._

    "a simple actor" should {
        "send back a duplicated message" in {
            // prepare
            val simpleActor =
                testKit.spawn(SimpleActor(), "simpleActor")
            val probe = testKit.createTestProbe[SimpleProtocol]()

            // scenario
            simpleActor ! SimpleMessage("Akka", probe.ref)

            // assertions
            probe.expectMessage(SimpleReply("AkkaAkka"))
        }

        "reply with favorite techs" in {
            // prepare
            val simpleActor =
                testKit.spawn(SimpleActor(), "simpleActor2")
            val probe = testKit.createTestProbe[SimpleProtocol]()

            // scenario
            simpleActor ! FavoriteTech(probe.ref)

            // assertions
            val favorites = probe.receiveMessages(2)
                .collect {
                    case SimpleReply(result) => result
                }

            favorites should contain allOf("Scala", "Akka")

        }
    }

    "a black hole actor" should {
        "never reply back" in {
            val blackhole = testKit.spawn(BlackHoleActor(), "blackHole")
            val probe = testKit.createTestProbe[SimpleProtocol]()

            blackhole ! SimpleMessage("hello?", probe.ref)
            blackhole ! SimpleMessage("anybody?", probe.ref)

            probe.expectNoMessage(1.second)
        }
    }
  
}

object TestingActors {
    sealed trait SimpleProtocol
    case class SimpleReply(result: String) 
        extends SimpleProtocol
    case class SimpleMessage(message: String, replyTo: ActorRef[SimpleReply])
        extends SimpleProtocol
    case class FavoriteTech(replyTo: ActorRef[SimpleProtocol])
        extends SimpleProtocol
    
    object SimpleActor {
        def apply(): Behavior[SimpleProtocol] = 
            Behaviors.receive { (ctx, msg) =>
            msg match {
                case SimpleMessage(message, replyTo) => 
                    ctx.log.info(s"[simple] will send back '$message' * 2")
                    replyTo ! SimpleReply(message * 2)//message + message
                    Behaviors.same
                case FavoriteTech(replyTo) => 
                    ctx.log.info(s"[simple] will tell what i like")
                    replyTo ! SimpleReply("Akka")
                    replyTo ! SimpleReply("Scala")
                    Behaviors.same
                case x: Any =>
                    ctx.log.info("other command $x")
                    Behaviors.same
            }
        }
    }

    object BlackHoleActor {
        def apply(): Behavior[SimpleProtocol] = 
            Behaviors.receive {(ctx, msg) =>
                msg match {
                    case x: Any =>
                        ctx.log.info("[blackhole] ...")
                        Behaviors.same
                }
            }
    }
}