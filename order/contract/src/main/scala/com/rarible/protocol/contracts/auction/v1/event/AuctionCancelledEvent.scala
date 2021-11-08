package com.rarible.protocol.contracts.auction.v1.event

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple1Type, UnitType}
import scalether.abi.{Event, Uint256Type}
import scalether.domain._
import scalether.domain.request.{LogFilter, TopicFilter}

import java.math.BigInteger

case class AuctionCancelledEvent(log: response.Log, auctionId: BigInteger)

object AuctionCancelledEvent {
  import TopicFilter.simple

  val event = Event("AuctionCancelled", List(Uint256Type), Tuple1Type(Uint256Type), UnitType)
  val id: Word = Word.apply("0x2809c7e17bf978fbc7194c0a694b638c4215e9140cacc6c38ca36010b45697df")

  def filter(auctionId: BigInteger): LogFilter =
    LogFilter(topics = List(simple(id), Uint256Type.encodeForTopic(auctionId)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[AuctionCancelledEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(AuctionCancelledEvent(_))

  def apply(log: response.Log): AuctionCancelledEvent = {
    assert(log.topics.head == id)


    val auctionId = event.indexed.type1.decode(log.topics(1), 0).value
    AuctionCancelledEvent(log, auctionId)
  }
}




