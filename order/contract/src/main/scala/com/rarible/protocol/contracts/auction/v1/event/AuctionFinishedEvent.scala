package com.rarible.protocol.contracts.auction.v1.event

import java.math.BigInteger

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

case class AuctionFinishedEvent(log: response.Log, auctionId: BigInteger, auction: (((Array[Byte], Array[Byte]), BigInteger), (Array[Byte], Array[Byte]), (BigInteger, Array[Byte], Array[Byte]), Address, Address, BigInteger, BigInteger, BigInteger, BigInteger, Array[Byte], Array[Byte]))

object AuctionFinishedEvent {
  import TopicFilter.simple

  val event = Event("AuctionFinished", List(Uint256Type, Tuple11Type(Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Tuple2Type(Bytes4Type, BytesType), Tuple3Type(Uint256Type, Bytes4Type, BytesType), AddressType, AddressType, Uint256Type, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)), Tuple1Type(Uint256Type), Tuple1Type(Tuple11Type(Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Tuple2Type(Bytes4Type, BytesType), Tuple3Type(Uint256Type, Bytes4Type, BytesType), AddressType, AddressType, Uint256Type, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)))
  val id: Word = Word.apply("0x35536caf6898f5cfbe09a980e5374d10f7ef130ad8366a9fc9a0e7b8c6c6649d")

  def filter(auctionId: BigInteger): LogFilter =
    LogFilter(topics = List(simple(id), Uint256Type.encodeForTopic(auctionId)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[AuctionFinishedEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(AuctionFinishedEvent(_))

  def apply(log: response.Log): AuctionFinishedEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val auctionId = event.indexed.type1.decode(log.topics(1), 0).value
    val auction = decodedData
    AuctionFinishedEvent(log, auctionId, auction)
  }
}



















