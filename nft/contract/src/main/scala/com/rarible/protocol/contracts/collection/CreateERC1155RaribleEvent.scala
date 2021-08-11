package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple3Type, UnitType}
import scalether.abi.{AddressType, Event, StringType}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

case class CreateERC1155RaribleEvent(log: response.Log, owner: Address, name: String, symbol: String)

object CreateERC1155RaribleEvent {
  import TopicFilter.simple

  val event = Event("CreateERC1155Rarible", List(AddressType, StringType, StringType), UnitType, Tuple3Type(AddressType, StringType, StringType))
  val id: Word = Word.apply("0xcc215b7682459c30faa0e854780165d503a7d62d22a9aaaad6334585dc63343e")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC1155RaribleEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC1155RaribleEvent(_))

  def apply(log: response.Log): CreateERC1155RaribleEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val owner = decodedData._1
    val name = decodedData._2
    val symbol = decodedData._3
    CreateERC1155RaribleEvent(log, owner, name, symbol)
  }
}




