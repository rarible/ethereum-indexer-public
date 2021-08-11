package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain.Word
import scalether.abi.{AddressType, Event, StringType}
import scalether.abi.tuple.{Tuple3Type, UnitType}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

case class CreateERC721RaribleUserEvent(log: response.Log, owner: Address, name: String, symbol: String)

object CreateERC721RaribleUserEvent {
  import TopicFilter.simple

  val event =
    Event("CreateERC721RaribleUser", List(AddressType, StringType, StringType), UnitType, Tuple3Type(AddressType, StringType, StringType))
  val id: Word = Word.apply("0xd901a467fa419f379a67636a1de44cc2ed772beb43a0c05fa1ddcad5d59e9913")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC721RaribleUserEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC721RaribleUserEvent(_))

  def apply(log: response.Log): CreateERC721RaribleUserEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val owner = decodedData._1
    val name = decodedData._2
    val symbol = decodedData._3
    CreateERC721RaribleUserEvent(log, owner, name, symbol)
  }
}
