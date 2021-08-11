package com.rarible.protocol.contracts.creators

import io.daonomic.rpc.domain.Word
import scalether.abi.array.VarArrayType
import scalether.abi.tuple.{Tuple2Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type, Uint96Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

import java.math.BigInteger

case class CreatorsEvent(log: response.Log, tokenId: BigInteger, creators: Array[(Address, BigInteger)])

//noinspection TypeAnnotation
object CreatorsEvent {
  import TopicFilter.simple

  val event = Event("Creators", List(Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint96Type))), UnitType, Tuple2Type(Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint96Type))))
  val id: Word = Word.apply("0x841ffb90d4cabdd1f16034f3fa831d79060febbb8167bdd54a49269365bdf78f")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreatorsEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreatorsEvent(_))

  def apply(log: response.Log): CreatorsEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val tokenId = decodedData._1
    val creators = decodedData._2
    CreatorsEvent(log, tokenId, creators)
  }
}