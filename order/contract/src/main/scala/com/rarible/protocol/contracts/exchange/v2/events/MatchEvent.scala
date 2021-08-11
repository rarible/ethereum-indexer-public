package com.rarible.protocol.contracts.exchange.v2.events

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

import java.math.BigInteger

case class MatchEvent(log: response.Log, leftHash: Array[Byte], rightHash: Array[Byte], leftMaker: Address, rightMaker: Address, newLeftFill: BigInteger, newRightFill: BigInteger, leftAsset: (Array[Byte], Array[Byte]), rightAsset: (Array[Byte], Array[Byte]))

//noinspection TypeAnnotation
object MatchEvent {
  import scalether.domain.request.TopicFilter.simple

  val event = Event("Match", List(Bytes32Type, Bytes32Type, AddressType, AddressType, Uint256Type, Uint256Type, Tuple2Type(Bytes4Type, BytesType), Tuple2Type(Bytes4Type, BytesType)), UnitType, Tuple8Type(Bytes32Type, Bytes32Type, AddressType, AddressType, Uint256Type, Uint256Type, Tuple2Type(Bytes4Type, BytesType), Tuple2Type(Bytes4Type, BytesType)))
  val id: Word = Word.apply("0x268820db288a211986b26a8fda86b1e0046281b21206936bb0e61c67b5c79ef4")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[MatchEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(MatchEvent(_))

  def apply(log: response.Log): MatchEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val leftHash = decodedData._1
    val rightHash = decodedData._2
    val leftMaker = decodedData._3
    val rightMaker = decodedData._4
    val newLeftFill = decodedData._5
    val newRightFill = decodedData._6
    val leftAsset = decodedData._7
    val rightAsset = decodedData._8
    MatchEvent(log, leftHash, rightHash, leftMaker, rightMaker, newLeftFill, newRightFill, leftAsset, rightAsset)
  }
}
