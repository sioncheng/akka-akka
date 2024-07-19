package part3

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit

import part2.ChildActorsExercise._
import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.actor.testkit.typed.CapturedLogEvent
import org.slf4j.event.Level

class SynchronousTestingSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  
    "A word counter master" should {
        "spawn a child upon reception of the initialize message" in {
            val master = BehaviorTestKit(WordCounterMaster())

            master.run(Initialize(1))

            val effect = master.expectEffectType[Spawned[WorkerProtocol]]

            effect.childName should equal("worker1")
        }

        "send a task to a child" in {
            val master = BehaviorTestKit(WordCounterMaster())

            master.run(Initialize(1))

            val eefect = master.expectEffectType[Spawned[WorkerProtocol]]

            val mailbox = TestInbox[UserProtocol]()

            // content of WordCountTask doesn't matter here
            // just to make sure the logic needed by WordCountReply happened
            //master.run(WordCountTask("Akka testing is pretty powerful", mailbox.ref))
            master.run(WordCountTask("", mailbox.ref))
            //mock
            master.run(WordCountReply(0, 5))

            mailbox.expectMessage(Reply(5))
        }

        "log messages" in {
            val master = BehaviorTestKit(WordCounterMaster())
            master.run(Initialize(1))
            master.logEntries() shouldBe Seq(CapturedLogEvent(Level.INFO, "[master] initializing with 1 children"))
        }
    }
}
