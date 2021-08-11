package com.rarible.protocol.contracts

import java.math.BigInteger

import com.rarible.contracts.erc721.TransferEvent
import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple1Type, Tuple2Type}
import scalether.abi.{AddressType, Event, Uint256Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

object TransferEventWithNotFullData {
  import TopicFilter.simple

  val event: Event[Tuple2Type[Address, Address], BigInteger] =
    Event("Transfer", List(AddressType, AddressType, Uint256Type), Tuple2Type(AddressType, AddressType), Tuple1Type(Uint256Type))
  val id: Word = Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef")

  def filter(from: Address, to: Address, tokenId: BigInteger): LogFilter =
    LogFilter(topics = List(simple(id), AddressType.encodeForTopic(from), AddressType.encodeForTopic(to), Uint256Type.encodeForTopic(tokenId)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[TransferEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(TransferEventWithNotFullData(_))

  def apply(log: response.Log): TransferEvent = {
    assert(log.topics.head == id)

    val from = event.indexed.type1.decode(log.topics(1), 0).value
    val to = event.indexed.type2.decode(log.topics(2), 0).value
    val tokenId = event.nonIndexed.decode(log.data, 0).value
    TransferEvent(log, from, to, tokenId)
  }
}
