package com.rarible.protocol.contracts.exchange.v2.events

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

import java.math.BigInteger

case class UpsertOrderEvent(log: response.Log, order: (Address, ((Array[Byte], Array[Byte]), BigInteger), Address, ((Array[Byte], Array[Byte]), BigInteger), BigInteger, BigInteger, BigInteger, Array[Byte], Array[Byte]))

//noinspection TypeAnnotation
object UpsertOrderEvent {
  import TopicFilter.simple

  val event = Event("UpsertOrder", List(Tuple9Type(AddressType, Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), AddressType, Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)), UnitType, Tuple1Type(Tuple9Type(AddressType, Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), AddressType, Tuple2Type(Tuple2Type(Bytes4Type, BytesType), Uint256Type), Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)))
  val id: Word = Word.apply("0x1acc8aca73f063aff7288a5f63824311301634d60a04d06437f67d5f0604bd19")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[UpsertOrderEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(UpsertOrderEvent(_))

  def apply(log: response.Log): UpsertOrderEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val order = decodedData
    UpsertOrderEvent(log, order)
  }
}
