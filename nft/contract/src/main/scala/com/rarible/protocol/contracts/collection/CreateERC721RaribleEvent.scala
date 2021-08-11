package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain.Word
import scalether.abi.{AddressType, Event, StringType}
import scalether.abi.tuple.{Tuple3Type, UnitType}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

case class CreateERC721RaribleEvent(log: response.Log, owner: Address, name: String, symbol: String)

object CreateERC721RaribleEvent {
  import TopicFilter.simple

  val event =
    Event("CreateERC721Rarible", List(AddressType, StringType, StringType), UnitType, Tuple3Type(AddressType, StringType, StringType))
  val id: Word = Word.apply("0xf05e55f0a9d205977ca8cc02236338b6a361376f404cf0b3019b2111964a01fd")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC721RaribleEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC721RaribleEvent(_))

  def apply(log: response.Log): CreateERC721RaribleEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val owner = decodedData._1
    val name = decodedData._2
    val symbol = decodedData._3
    CreateERC721RaribleEvent(log, owner, name, symbol)
  }
}