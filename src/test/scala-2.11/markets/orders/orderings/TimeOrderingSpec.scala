/*
Copyright 2015 David R. Pugh, J. Doyne Farmer, and Dan F. Tang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package markets.orders.orderings

import akka.actor.ActorSystem
import akka.testkit.TestKit
import markets.orders._
import markets.tradables.TestTradable
import org.scalatest.{BeforeAndAfterAll, FeatureSpecLike, GivenWhenThen, Matchers}

import scala.collection.mutable


class TimeOrderingSpec extends TestKit(ActorSystem("TimeOrderingSpec")) with
  FeatureSpecLike with
  GivenWhenThen with
  Matchers with
  BeforeAndAfterAll {

  /** Shutdown actor system when finished. */
  override def afterAll(): Unit = {
    system.terminate()
  }

  val testTradable = TestTradable("AAPL")

  feature("A OrderedBook with TimeOrdering should maintain time priority") {

    scenario("A new order lands in SortedAskOrderBook with existing orders.") {

      Given("An ask order book that contains existing orders")

      val existingAsk1 = MarketAskOrder(testActor, 100, 10, testTradable)
      val existingAsk2 = LimitAskOrder(testActor, 10, 1, 15, testTradable)
      val askOrderBook = mutable.TreeSet[AskOrderLike]()(AskTimeOrdering())

      askOrderBook +=(existingAsk1, existingAsk2)

      When("additional orders land in the order book the order book should maintain time priority.")

      // initial head of the order book
      askOrderBook.head should be(existingAsk1)

      // incoming order with lower timestamp should move to the head of the book
      val incomingAsk1 = LimitAskOrder(testActor, 10, 1, 9, testTradable)
      askOrderBook += incomingAsk1
      askOrderBook.head should be(incomingAsk1)
      
      // generic incoming order should not change head of the book
      val incomingAsk2 = MarketAskOrder(testActor, 10, 13, testTradable)
      askOrderBook += incomingAsk1
      askOrderBook.head should be(incomingAsk1)

    }
    
    scenario("A new order lands in SortedBidOrderBook with existing orders.") {

      Given("An bid order book that contains existing orders")

      val existingBid1 = MarketBidOrder(testActor, 100, 10, testTradable)
      val existingBid2 = LimitBidOrder(testActor, 10, 1, 15, testTradable)
      val bidOrderBook = mutable.TreeSet[BidOrderLike]()(BidTimeOrdering())

      bidOrderBook +=(existingBid1, existingBid2)

      When("additional orders land in the order book the order book should maintain time priority.")

      // initial head of the order book
      bidOrderBook.head should be(existingBid1)

      // incoming order with lower timestamp should move to the head of the book
      val incomingBid1 = LimitBidOrder(testActor, 10, 1, 9, testTradable)
      bidOrderBook += incomingBid1
      bidOrderBook.head should be(incomingBid1)

      // generic incoming order should not change head of the book
      val incomingBid2 = MarketBidOrder(testActor, 10, 13, testTradable)
      bidOrderBook += incomingBid2
      bidOrderBook.head should be(incomingBid1)

    }
  }
}
