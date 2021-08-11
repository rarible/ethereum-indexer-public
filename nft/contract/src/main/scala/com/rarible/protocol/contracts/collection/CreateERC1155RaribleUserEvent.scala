package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple3Type, UnitType}
import scalether.abi.{AddressType, Event, StringType}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

case class CreateERC1155RaribleUserEvent(log: response.Log, owner: Address, name: String, symbol: String)

object CreateERC1155RaribleUserEvent {
  import TopicFilter.simple

  val event =
    Event("CreateERC1155RaribleUser", List(AddressType, StringType, StringType), UnitType, Tuple3Type(AddressType, StringType, StringType))
  val id: Word = Word.apply("0x7da6bc204c8c4856a6aff786a6cb81c59477c782191dc51837d644a8ad50f2cc")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC1155RaribleUserEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC1155RaribleUserEvent(_))

  def apply(log: response.Log): CreateERC1155RaribleUserEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val owner = decodedData._1
    val name = decodedData._2
    val symbol = decodedData._3
    CreateERC1155RaribleUserEvent(log, owner, name, symbol)
  }
}


