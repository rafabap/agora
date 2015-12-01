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
package markets.clearing.engines

import markets.clearing.strategies.PriceFormationStrategy
import markets.fills.Fill
import markets.orders.Order

import scala.collection.immutable


/** Base trait for all matching engines.
  *
  * @note A `MatchingEngineLike` object should handle any necessary queuing of ask and bid orders,
  *       order execution (specifically price formation and quantity determination), and generate
  *       fills orders.
  */
trait MatchingEngineLike {
  this: PriceFormationStrategy =>

  /** MatchingEngine should maintain some collection of orders. */
  protected def orderBook: immutable.Iterable[Order]

  /** Fill an incoming order.
    *
    * @param incomingOrder the order to be fills.
    * @return a collection of fills orders.
    * @note Depending on size of the incoming order and the state of the market when the order is
    *       received, a single incoming order may generate several fills orders.
    */
  def fill(incomingOrder: Order): Option[immutable.Iterable[Fill]]

}
