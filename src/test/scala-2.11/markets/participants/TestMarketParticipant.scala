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
package markets.participants

import akka.actor.ActorRef

import java.util.UUID

import markets.BaseActor
import markets.tradables.Tradable

import scala.collection.immutable


class TestMarketParticipant extends BaseActor with MarketParticipantLike {

  var markets = immutable.Map.empty[Tradable, ActorRef]

  var outstandingOrders = immutable.Set.empty[UUID]

  def receive: Receive = {
    marketParticipantBehavior orElse baseActorBehavior
  }

}
