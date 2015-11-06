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
package markets

import akka.actor.{ActorRef, Props, Actor}
import markets.clearing.ClearingMechanismActor
import markets.clearing.engines.MatchingEngineLike
import markets.tradables.Tradable


class MarketActor(matchingEngine: MatchingEngineLike,
                  settlementMechanism: ActorRef,
                  val tradable: Tradable) extends Actor
  with MarketLike {

  /** Each `MarketActor` has a unique clearing mechanism. */
  val clearingMechanism: ActorRef = {
    context.actorOf(ClearingMechanismActor.props(matchingEngine, settlementMechanism),
      "clearing-mechanism")
  }

}


object MarketActor {

  def apply(matchingEngine: MatchingEngineLike, settlementMechanism: ActorRef, tradable: Tradable): MarketActor = {
    new MarketActor(matchingEngine, settlementMechanism, tradable)
  }

  def props(matchingEngine: MatchingEngineLike, settlementMechanism: ActorRef, tradable: Tradable): Props = {
    Props(new MarketActor(matchingEngine, settlementMechanism, tradable))
  }

}
