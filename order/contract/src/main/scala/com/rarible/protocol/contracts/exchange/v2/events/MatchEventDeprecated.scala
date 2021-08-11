package com.rarible.protocol.contracts.exchange.v2.events

import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple6Type, UnitType}
import scalether.abi.{AddressType, Bytes32Type, Event, Uint256Type}
import scalether.domain.request.LogFilter
import scalether.domain.{Address, response}

import java.math.BigInteger

case class MatchEventDeprecated(log: response.Log, leftHash: Array[Byte], rightHash: Array[Byte], leftMaker: Address, rightMaker: Address, newLeftFill: BigInteger, newRightFill: BigInteger)

//noinspection TypeAnnotation
object MatchEventDeprecated {
  import scalether.domain.request.TopicFilter.simple

  val event = Event("Match", List(Bytes32Type, Bytes32Type, AddressType, AddressType, Uint256Type, Uint256Type), UnitType, Tuple6Type(Bytes32Type, Bytes32Type, AddressType, AddressType, Uint256Type, Uint256Type))
  val id: Word = Word.apply("0x6bd3176da6f0aab285937ad5ead5f53b9036f403cfad62f77e4e0f80b5a7e18d")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[MatchEventDeprecated] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(MatchEventDeprecated(_))

  def apply(log: response.Log): MatchEventDeprecated = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val leftHash = decodedData._1
    val rightHash = decodedData._2
    val leftMaker = decodedData._3
    val rightMaker = decodedData._4
    val newLeftFill = decodedData._5
    val newRightFill = decodedData._6
    MatchEventDeprecated(log, leftHash, rightHash, leftMaker, rightMaker, newLeftFill, newRightFill)
  }
}