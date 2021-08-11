package com.rarible.protocol.contracts.royalties

import io.daonomic.rpc.domain.Word
import scalether.abi.array.VarArrayType
import scalether.abi.tuple.{Tuple2Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type, Uint96Type}
import scalether.domain.Address
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.response.Log

import java.math.BigInteger

case class RoyaltiesSetEvent(log: Log, tokenId: BigInteger, royalties: Array[(Address, BigInteger)])

object RoyaltiesSetEvent {
  import TopicFilter.simple

  val event: Event[UnitType.type, (BigInteger, Array[(Address, BigInteger)])] =
    Event("RoyaltiesSet", List(Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint96Type))), UnitType, Tuple2Type(Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint96Type))))
  val id: Word = Word.apply("0x3fa96d7b6bcbfe71ef171666d84db3cf52fa2d1c8afdb1cc8e486177f208b7df")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[RoyaltiesSetEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(RoyaltiesSetEvent(_))

  def apply(log: Log): RoyaltiesSetEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val tokenId = decodedData._1
    val royalties = decodedData._2
    RoyaltiesSetEvent(log, tokenId, royalties)
  }
}