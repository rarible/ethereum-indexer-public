package com.rarible.protocol.contracts.auction.v1.event

import java.math.BigInteger

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

case class BidPlacedEvent(log: response.Log, auctionId: BigInteger, bid: (BigInteger, Array[Byte], Array[Byte]), endTime: BigInteger)

object BidPlacedEvent {
  import TopicFilter.simple

  val event = Event("BidPlaced", List(Uint256Type, Tuple3Type(Uint256Type, Bytes4Type, BytesType), Uint256Type), Tuple1Type(Uint256Type), Tuple2Type(Tuple3Type(Uint256Type, Bytes4Type, BytesType), Uint256Type))
  val id: Word = Word.apply("0x9d89b26c88af0666e0502f71246ce92ea5ee513d508840b43aeb2eb22fac3ed1")

  def filter(auctionId: BigInteger): LogFilter =
    LogFilter(topics = List(simple(id), Uint256Type.encodeForTopic(auctionId)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[BidPlacedEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(BidPlacedEvent(_))

  def apply(log: response.Log): BidPlacedEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val auctionId = event.indexed.type1.decode(log.topics(1), 0).value
    val bid = decodedData._1
    val endTime = decodedData._2
    BidPlacedEvent(log, auctionId, bid, endTime)
  }
}















