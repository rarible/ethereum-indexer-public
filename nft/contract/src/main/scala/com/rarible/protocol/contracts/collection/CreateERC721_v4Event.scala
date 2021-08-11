package com.rarible.protocol.contracts.collection

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

case class CreateERC721_v4Event(log: response.Log, creator: Address, name: String, symbol: String)

object CreateERC721_v4Event {
  import TopicFilter.simple

  val event: Event[Tuple1Type[Address], (String, String)] =
    Event("CreateERC721_v4", List(AddressType, StringType, StringType), Tuple1Type(AddressType), Tuple2Type(StringType, StringType))
  val id: Word = Word.apply("0x565f2552ba3b09c1a27ca36ec97f816d9f12f464c3f7f145c28b527057df0ac7")

  def filter(creator: Address): LogFilter =
    LogFilter(topics = List(simple(id), AddressType.encodeForTopic(creator)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[CreateERC721_v4Event] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(CreateERC721_v4Event(_))

  def apply(log: response.Log): CreateERC721_v4Event = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val creator = event.indexed.type1.decode(log.topics(1), 0).value
    val name = decodedData._1
    val symbol = decodedData._2
    CreateERC721_v4Event(log, creator, name, symbol)
  }
}