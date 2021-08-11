package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

case class CreateERC1155_v1Event(log: response.Log, creator: Address, name: String, symbol: String)

object CreateERC1155_v1Event {
  import TopicFilter.simple

  val event: Event[Tuple1Type[Address], (String, String)] =
    Event("CreateERC1155_v1", List(AddressType, StringType, StringType), Tuple1Type(AddressType), Tuple2Type(StringType, StringType))
  val id: Word = Word.apply("0x658fd9a983a35f4a0bb697abb2c6971d688010d8bc264928a164ae391b87472c")

  def filter(creator: Address): LogFilter =
    LogFilter(topics = List(simple(id), AddressType.encodeForTopic(creator)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC1155_v1Event] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC1155_v1Event(_))

  def apply(log: response.Log): CreateERC1155_v1Event = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val creator = event.indexed.type1.decode(log.topics(1), 0).value
    val name = decodedData._1
    val symbol = decodedData._2
    CreateERC1155_v1Event(log, creator, name, symbol)
  }
}