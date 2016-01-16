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
package markets.engines

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestKit

import java.util.UUID

import markets.orders.limit.{LimitAskOrder, LimitBidOrder}
import markets.orders.market.{MarketAskOrder, MarketBidOrder}
import markets.orders.orderings.ask.AskPriceTimeOrdering
import markets.orders.orderings.bid.BidPriceTimeOrdering
import markets.tradables.Security
import org.scalatest.{BeforeAndAfterAll, FeatureSpecLike, GivenWhenThen, Matchers}

import scala.collection.immutable
import scala.util.Random


class CDAMatchingEngineSpec extends TestKit(ActorSystem("CDAMatchingEngineSpec")) with
  FeatureSpecLike with
  GivenWhenThen with
  Matchers with
  BeforeAndAfterAll {

  /** Shutdown actor system when finished. */
  override def afterAll(): Unit = {
    system.terminate()
  }

  def randomLong(prng: Random, lower: Long = 1, upper: Long = Long.MaxValue): Long = {
    math.abs(prng.nextLong()) % (upper - lower) + lower
  }

  val testTradable: Security = Security("AAPL")

  val askOrderIssuer: ActorRef = testActor

  val bidOrderIssuer: ActorRef = testActor

  def uuid(): UUID = {
    UUID.randomUUID()
  }

  feature("A CDAMatchingEngine matching engine should be able to generate matches orders") {

    val prng: Random = new Random()

    scenario("A new limit ask order lands in an empty order book.") {

      Given("a matching engine with an empty ask order book...")

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      When("a LimitAskOrder arrives...")
      val askOrder = LimitAskOrder(askOrderIssuer, randomLong(prng), randomLong(prng), randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("it should land in the ask order book")
      matchings should be(None)
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(askOrder))

    }

    scenario("A new market ask order lands in an empty order book.") {

      Given("a matching engine with an empty ask order book...")

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      When("a MarketAskOrder arrives...")
      val askOrder = MarketAskOrder(askOrderIssuer, randomLong(prng), randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("it should land in the ask order book.")
      matchings should be(None)
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(askOrder))

    }

    scenario("A new limit bid order lands in an empty order book.") {

      Given("a matching engine with an empty bid order book...")

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      When("a LimitBidOrder arrives...")
      val bidOrder = LimitBidOrder(bidOrderIssuer, randomLong(prng), randomLong(prng), randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("it should land in the bid order book.")

      matchings should be(None)
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(bidOrder))

    }

    scenario("A new market bid order lands in an empty order book.") {

      Given("a matching engine with an empty bid order book...")

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      When("a MarketBidOrder arrives...")
      val bidOrder = MarketBidOrder(bidOrderIssuer, randomLong(prng), randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("it should land in the bid order book.")

      matchings should be(None)
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(bidOrder))

    }

    scenario("A limit ask order crosses an existing limit bid order with the same quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val quantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, quantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming LimitAskOrder crosses the existing limit bid order...")
      val askPrice = randomLong(prng, upper = bidPrice)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, quantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching at the bid price.")

      val matching = Matching(askOrder, bidOrder, bidPrice, quantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that order books are now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A limit ask order crosses an existing market bid order with the same quantity.") {

      val initialPrice = 1
      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, initialPrice)

      Given("a matching engine with only existing market bid order on its book...")
      val quantity = randomLong(prng)
      val bidOrder = MarketBidOrder(bidOrderIssuer, quantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming LimitAskOrder crosses the existing limit bid order...")
      val askPrice = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, quantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching at the reference price.")

      val price = math.max(initialPrice, askPrice)
      val matching = Matching(askOrder, bidOrder, price, quantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that order books are now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A market ask order crosses an existing limit bid order with the same quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val quantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, quantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming MarketAskOrder crosses the existing limit bid order...")
      val askOrder = MarketAskOrder(askOrderIssuer, quantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching at the bid price.")

      val matching = Matching(askOrder, bidOrder, bidPrice, quantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that order books are now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A limit ask order crosses an existing limit bid order with a greater quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val bidQuantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming LimitAskOrder crosses the existing limit bid order...")
      val askPrice = randomLong(prng, upper = bidPrice)
      val askQuantity = randomLong(prng, upper = bidQuantity)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching")

      val residualBidQuantity = bidQuantity - askQuantity
      val (_, residualBidOrder) = bidOrder.split(residualBidQuantity)
      val matching = Matching(askOrder, bidOrder, bidPrice, askQuantity, None, Some(residualBidOrder))
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder))

    }

    scenario("A limit ask order crosses an existing market bid order with a greater quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing market and limit bid orders on its book...")
      val bidPrice = randomLong(prng)
      val bidQuantity = randomLong(prng)
      val limitBidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng),
        testTradable, uuid())
      matchingEngine.findMatch(limitBidOrder)

      val marketBidOrder = MarketBidOrder(bidOrderIssuer, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(marketBidOrder)

      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(marketBidOrder, limitBidOrder))

      When("an incoming LimitAskOrder crosses the existing market bid order...")
      val askPrice = randomLong(prng, upper = bidPrice)
      val askQuantity = randomLong(prng, upper = bidQuantity)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching at the best limit price.")

      val residualBidQuantity = bidQuantity - askQuantity
      val (_, residualBidOrder) = marketBidOrder.split(residualBidQuantity)
      val matching = Matching(askOrder, marketBidOrder, bidPrice, askQuantity, None, Some(residualBidOrder))
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder, limitBidOrder))

    }

    scenario("A market ask order crosses an existing limit bid order with a greater quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val bidQuantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming MarketAskOrder crosses the existing limit bid order...")
      val askQuantity = randomLong(prng, upper = bidQuantity)
      val askOrder = MarketAskOrder(askOrderIssuer, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching")
      val residualBidQuantity = bidQuantity - askQuantity
      val (_, residualBidOrder) = bidOrder.split(residualBidQuantity)
      val matching = Matching(askOrder, bidOrder, bidPrice, askQuantity, None, Some(residualBidOrder))
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder))

    }

    scenario("A limit ask order crosses an existing limit bid order with a lesser quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val bidQuantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming LimitAskOrder crosses the existing limit bid order...")
      val askPrice = randomLong(prng, upper = bidPrice)
      val askQuantity = randomLong(prng, lower = bidQuantity)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching")

      val residualAskQuantity = askQuantity - bidQuantity
      val (_, residualAskOrder) = askOrder.split(residualAskQuantity)
      val matching = Matching(askOrder, bidOrder, bidPrice, bidQuantity, Some(residualAskOrder), None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also need to check that residual ask order landed in the book
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(residualAskOrder))

      // also should check that bid order book is now empty
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)
    }

    scenario("A market ask order crosses an existing limit bid order with a lesser quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val bidQuantity = randomLong(prng)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("an incoming MarketAskOrder crosses the existing limit bid order...")
      val askQuantity = randomLong(prng, lower = bidQuantity)
      val askOrder = MarketAskOrder(askOrderIssuer, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching")

      val residualAskQuantity = askQuantity - bidQuantity
      val (_, residualAskOrder) = askOrder.split(residualAskQuantity)
      val matching = Matching(askOrder, bidOrder, bidPrice, bidQuantity, Some(residualAskOrder), None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also need to check that residual ask order landed in the book
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(residualAskOrder))

      // also should check that bid order book is now empty
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)
    }

    scenario("A market ask order crosses an existing market bid order with the same quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit bid order on its book...")
      val bidPrice = randomLong(prng)
      val limitBidQuantity = randomLong(prng)
      val limitBidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, limitBidQuantity,
        randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(limitBidOrder)

      val marketBidQuantity = randomLong(prng)
      val marketBidOrder = MarketBidOrder(bidOrderIssuer, marketBidQuantity, randomLong(prng),
        testTradable, uuid())
      matchingEngine.findMatch(marketBidOrder)

      When("an incoming MarketAskOrder crosses the existing market bid order...")
      val askOrder = MarketAskOrder(askOrderIssuer, marketBidQuantity, randomLong(prng),
        testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching")

      val matching = Matching(askOrder, marketBidOrder, bidPrice, marketBidQuantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(limitBidOrder))

      matchingEngine.askOrderBook.isEmpty should be(true)

    }

    scenario("A limit bid order crosses an existing limit ask order with the same quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val quantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, quantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming LimitBidOrder crosses the existing limit ask order...")
      val bidPrice = randomLong(prng, lower = askPrice)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, quantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching")

      val matching = Matching(askOrder, bidOrder, askPrice, quantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that order books are now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A market bid order crosses an existing limit ask order with the same quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val quantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, quantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming MarketBidOrder crosses the existing limit ask order...")
      val bidOrder = MarketBidOrder(bidOrderIssuer, quantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching at the ask price.")

      val matching = Matching(askOrder, bidOrder, askPrice, quantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that order books are now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A market bid order crosses an existing market ask order with the same quantity.") {
      val referencePrice = 1
      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, referencePrice)

      Given("a matching engine with existing limit and market ask orders on its book...")
      val askPrice = randomLong(prng)
      val limitQuantity = randomLong(prng)
      val limitAskOrder = LimitAskOrder(askOrderIssuer, askPrice, limitQuantity, randomLong(prng),
        testTradable, uuid())
      matchingEngine.findMatch(limitAskOrder)

      val marketQuantity = randomLong(prng)
      val marketAskOrder = MarketAskOrder(askOrderIssuer, marketQuantity, randomLong(prng),
        testTradable, uuid())
      matchingEngine.findMatch(marketAskOrder)

      When("an incoming MarketBidOrder crosses an existing market ask order...")
      val bidOrder = MarketBidOrder(bidOrderIssuer, marketQuantity, randomLong(prng), testTradable,
        uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching at the lesser of the best limit ask " +
        "price and the reference price")

      val price = math.min(limitAskOrder.price, referencePrice)
      val matching = Matching(marketAskOrder, bidOrder, price, marketQuantity, None, None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      matchingEngine.askOrderBook.toSeq should be(immutable.Seq(limitAskOrder))
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A limit bid order crosses an existing limit ask order with a greater quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming LimitBidOrder crosses the existing limit ask order...")
      val bidPrice = randomLong(prng, lower = askPrice)
      val bidQuantity = randomLong(prng, upper = askQuantity)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching")

      val residualAskQuantity = askQuantity - bidQuantity
      val (_, residualAskOrder) = askOrder.split(residualAskQuantity)
      val matching = Matching(askOrder, bidOrder, askPrice, bidQuantity, Some(residualAskOrder), None)
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also should check that bid order book is now empty
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual ask order landed in the book
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(residualAskOrder))

    }

    scenario("A limit ask order crosses an existing market bid order with a lesser quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing market and limit bid orders on its book...")
      val bidPrice = randomLong(prng)
      val limitBidQuantity = randomLong(prng)
      val limitBidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, limitBidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(limitBidOrder)

      val marketBidQuantity = randomLong(prng, upper = limitBidQuantity)
      val marketBidOrder = MarketBidOrder(bidOrderIssuer, marketBidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(marketBidOrder)

      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(marketBidOrder, limitBidOrder))

      When("an incoming LimitAskOrder crosses the existing market bid order...")
      val askPrice = randomLong(prng, upper = bidPrice)
      val askQuantity = randomLong(prng, marketBidQuantity, limitBidQuantity)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(askOrder)

      Then("the matching engine should generate a Matching at the best limit price.")

      val residualAskQuantity = askQuantity - marketBidQuantity
      val (_, residualAskOrder) = askOrder.split(residualAskQuantity)
      val partialFill = Matching(askOrder, marketBidOrder, bidPrice, marketBidQuantity, Some(residualAskOrder), None)

      val residualBidQuantity = limitBidQuantity - residualAskQuantity
      val (_, residualBidOrder) = limitBidOrder.split(residualBidQuantity)
      val totalFill = Matching(residualAskOrder, limitBidOrder, bidPrice, residualAskQuantity,
        None, Some(residualBidOrder))
      val expectedFilledOrders = immutable.Queue(partialFill, totalFill)
      matchings should equal(Some(expectedFilledOrders))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder))

    }

    scenario("A market bid order crosses an existing limit ask order with a greater quantity.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming MarketBidOrder crosses the existing limit ask order...")
      val bidQuantity = randomLong(prng, upper = askQuantity)
      val bidOrder = MarketBidOrder(bidOrderIssuer, bidQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching")

      val residualAskQuantity = askQuantity - bidQuantity
      val (_, residualAskOrder) = askOrder.split(residualAskQuantity)
      val matching = Matching(askOrder, bidOrder, askPrice, bidQuantity, Some(residualAskOrder), None)
      matchings should equal(Some(immutable.Queue(matching)))

      // also should check that bid order book is now empty
      matchingEngine.bidOrderBook.toSeq.isEmpty should be(true)

      // also need to check that residual ask order landed in the book
      matchingEngine.askOrderBook.toSeq should equal(immutable.Seq(residualAskOrder))

    }

    scenario("A limit bid order crosses an existing limit ask order with a lesser quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming LimitBidOrder crosses the existing limit ask order...")
      val bidPrice = randomLong(prng, lower = askPrice)
      val bidQuantity = randomLong(prng, lower = askQuantity)
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching")

      val residualBidQuantity = bidQuantity - askQuantity
      val (_, residualBidOrder) = bidOrder.split(residualBidQuantity)
      val matching = Matching(askOrder, bidOrder, askPrice, askQuantity, None, Some(residualBidOrder))
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
    }

    scenario("A market bid order crosses an existing limit ask order with a lesser quantity.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a matching engine with an existing limit ask order on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      When("an incoming MarketBidOrder crosses the existing limit ask order...")
      val bidQuantity = randomLong(prng, lower = askQuantity)
      val bidOrder = MarketBidOrder(bidOrderIssuer, bidQuantity, randomLong(prng), testTradable, uuid())
      val matchings = matchingEngine.findMatch(bidOrder)

      Then("the matching engine should generate a Matching")

      val residualBidQuantity = bidQuantity - askQuantity
      val (_, residualBidOrder) = bidOrder.split(residualBidQuantity)
      val matching = Matching(askOrder, bidOrder, askPrice, askQuantity, None, Some(residualBidOrder))
      matchings should equal(Some(immutable.Queue[Matching](matching)))

      // also need to check that residual bid order landed in the book
      matchingEngine.bidOrderBook.toSeq should equal(immutable.Seq(residualBidOrder))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)
    }
  }

  feature("A CDAMatchingEngine should be able to remove and order from the order book.") {

    val prng: Random = new Random()

    scenario("A CDAMatchingEngine attempts to remove an existing order from its order book.") {

      val matchingEngine =  CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a CDAMatchingEngine with an existing limit order on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      Then("...the CDAMatchingEngine should be able to remove that order.")
      val result = matchingEngine.remove(askOrder)
      result should be(Some(askOrder))

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

    }

    scenario("A CDAMatchingEngine attempts to remove an order from its order book.") {

      val matchingEngine = CDAMatchingEngine(AskPriceTimeOrdering, BidPriceTimeOrdering, 1)

      Given("a CDAMatchingEngine with an existing orders on its book...")
      val askPrice = randomLong(prng)
      val askQuantity = randomLong(prng)
      val askOrder = LimitAskOrder(askOrderIssuer, askPrice, askQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(askOrder)

      val bidPrice = randomLong(prng, lower = askPrice)
      val bidQuantity = askQuantity
      val bidOrder = LimitBidOrder(bidOrderIssuer, bidPrice, bidQuantity, randomLong(prng), testTradable, uuid())
      matchingEngine.findMatch(bidOrder)

      When("...the CDAMatchingEngine to remove and order that has already been filled...")
      val result = matchingEngine.remove(askOrder)
      result should be(None)

      // also should check that ask order book is now empty
      matchingEngine.askOrderBook.toSeq.isEmpty should be(true)

    }
  }
}
