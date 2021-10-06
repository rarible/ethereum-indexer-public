package com.rarible.protocol.erc20.contract

import com.rarible.contracts.interfaces.weth9.DepositEvent
import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple2Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.response

object DepositEventByLogData {

  import TopicFilter.simple

  val event = Event("Deposit", List(AddressType, Uint256Type), UnitType, Tuple2Type(AddressType, Uint256Type))
  val id: Word = Word.apply("0xe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[DepositEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(DepositEventByLogData(_))

  def apply(log: response.Log): DepositEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val dst = decodedData._1
    val wad = decodedData._2
    DepositEvent(log, dst, wad)
  }
}
