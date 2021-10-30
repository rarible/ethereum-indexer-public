package com.rarible.protocol.contracts.auction.v1.event

import io.daonomic.rpc.domain.Word
import scalether.abi.{AddressType, Bytes4Type, BytesType, Event, Uint256Type}
import scalether.abi.tuple.{Tuple11Type, Tuple1Type, Tuple2Type, Tuple3Type}
import scalether.domain._
import scalether.domain.request.TopicFilter.simple
import scalether.domain.request.{LogFilter, TopicFilter}

import java.math.BigInteger

case class AuctionCreatedEvent(log: response.Log, auctionId: BigInteger, auction: (((Array[Byte], Array[Byte]), BigInteger), (Array[Byte], Array[Byte]), (BigInteger, Array[Byte], Array[Byte]), Address, Address, BigInteger, BigInteger, BigInteger, BigInteger, Array[Byte], Array[Byte]))

object AuctionCreatedEvent {
  import TopicFilter.simple

  val event = Event("AuctionCreated", List(Uint256Type, Tuple11Type(Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Tuple2Type(Bytes4Type, BytesType), Tuple3Type(Uint256Type, Bytes4Type, BytesType), AddressType, AddressType, Uint256Type, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)), Tuple1Type(Uint256Type), Tuple1Type(Tuple11Type(Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Tuple2Type(Bytes4Type, BytesType), Tuple3Type(Uint256Type, Bytes4Type, BytesType), AddressType, AddressType, Uint256Type, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)))
  val id: Word = Word.apply("0x18f98bd854a0ed0d17c428b7f2c828ef9ddfc640f1e2d17d4150d03d54f76c9e")

  def filter(auctionId: BigInteger): LogFilter =
    LogFilter(topics = List(simple(id), Uint256Type.encodeForTopic(auctionId)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[AuctionCreatedEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(AuctionCreatedEvent(_))

  def apply(log: response.Log): AuctionCreatedEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val auctionId = event.indexed.type1.decode(log.topics(1), 0).value
    val auction = decodedData
    AuctionCreatedEvent(log, auctionId, auction)
  }
}


