/*
Copyright 2016 ScalABM

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
package markets.orders.limit

import markets.MarketsTestKit
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

import scala.util.Random


class LimitAskOrderSpec extends FeatureSpec
  with MarketsTestKit
  with GivenWhenThen
  with Matchers {

  val prng: Random = new Random()

  feature("A LimitAskOrder should be able to cross with other orders.") {

    val askOrder = randomAskOrder(marketOrderProbability = 0.0, tradable = validTradable)

    scenario("A LimitAskOrder should cross with any MarketBidOrder.") {
      val bidOrder = randomBidOrder(marketOrderProbability = 1.0, tradable = validTradable)
      assert(askOrder.crosses(bidOrder))
    }

    scenario("A LimitAskOrder should cross with any LimitBidOrder with a higher price.") {
      val bidOrder = randomBidOrder(marketOrderProbability = 0.0, minimumPrice = askOrder.price, tradable = validTradable)
      assert(askOrder.crosses(bidOrder))
    }

    scenario("A LimitAskOrder should not cross with any LimitBidOrder with a lower price.") {
      val bidOrder = randomBidOrder(marketOrderProbability = 0.0, maximumPrice = askOrder.price, tradable = validTradable)
      assert(!askOrder.crosses(bidOrder))
    }

    scenario("A LimitAskOrder should not cross with any LimitBidOrder for another tradable.") {
      val bidOrder = randomBidOrder(marketOrderProbability = 0.0, minimumPrice = askOrder.price, tradable = invalidTradable)
      intercept[MatchError](
        askOrder.crosses(bidOrder)
      )

    }
  }

}
