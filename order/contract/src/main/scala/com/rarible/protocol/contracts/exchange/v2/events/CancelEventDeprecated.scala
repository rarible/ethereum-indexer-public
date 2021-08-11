package com.rarible.protocol.contracts.exchange.v2.events

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple1Type, UnitType}
import scalether.abi.{Bytes32Type, Event}
import scalether.domain.request.LogFilter
import scalether.domain.response

case class CancelEventDeprecated(log: response.Log, hash: Array[Byte])

//noinspection TypeAnnotation
object CancelEventDeprecated {
  import scalether.domain.request.TopicFilter.simple

  val event = Event("Cancel", List(Bytes32Type), UnitType, Tuple1Type(Bytes32Type))
  val id: Word = Word.apply("0xe8d9861dbc9c663ed3accd261bbe2fe01e0d3d9e5f51fa38523b265c7757a93a")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CancelEventDeprecated] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CancelEventDeprecated(_))

  def apply(log: response.Log): CancelEventDeprecated = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val hash = decodedData
    CancelEventDeprecated(log, hash)
  }
}