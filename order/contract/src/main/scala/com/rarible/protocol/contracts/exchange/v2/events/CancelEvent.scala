package com.rarible.protocol.contracts.exchange.v2.events

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple2Type, Tuple4Type, UnitType}
import scalether.abi._
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

case class CancelEvent(log: response.Log, hash: Array[Byte], maker: Address, makeAssetType: (Array[Byte], Array[Byte]), takeAssetType: (Array[Byte], Array[Byte]))

//noinspection TypeAnnotation
object CancelEvent {
  import TopicFilter.simple

  val event = Event("Cancel", List(Bytes32Type, AddressType, Tuple2Type(Bytes4Type, BytesType), Tuple2Type(Bytes4Type, BytesType)), UnitType, Tuple4Type(Bytes32Type, AddressType, Tuple2Type(Bytes4Type, BytesType), Tuple2Type(Bytes4Type, BytesType)))
  val id: Word = Word.apply("0xbbdc98cb2835f4f846e6a63700d0498b4674f0e8858fd50c6379314227afa04e")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CancelEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CancelEvent(_))

  def apply(log: response.Log): CancelEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val hash = decodedData._1
    val maker = decodedData._2
    val makeAssetType = decodedData._3
    val takeAssetType = decodedData._4
    CancelEvent(log, hash, maker, makeAssetType, takeAssetType)
  }
}
