package com.rarible.protocol.contracts.creators

import io.daonomic.rpc.domain.Word
import scalether.abi.array.VarArrayType
import scalether.abi.tuple.{Tuple1Type, Tuple2Type}
import scalether.abi.{AddressType, Event, Uint256Type, Uint96Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.response

//noinspection TypeAnnotation
object CreatorsIndexedEvent {
  import TopicFilter.simple

  val event = Event("Creators", List(Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint96Type))), Tuple1Type(Uint256Type), Tuple1Type(VarArrayType(Tuple2Type(AddressType, Uint96Type))))
  val id: Word = Word.apply("0x841ffb90d4cabdd1f16034f3fa831d79060febbb8167bdd54a49269365bdf78f")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreatorsEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreatorsEvent(_))

  def apply(log: response.Log): CreatorsEvent = {
    assert(log.topics.head == id)

    val tokenId = Uint256Type.decode(log.topics(1), 0).value
    val creators = event.decode(log.data)
    CreatorsEvent(log, tokenId, creators)
  }
}