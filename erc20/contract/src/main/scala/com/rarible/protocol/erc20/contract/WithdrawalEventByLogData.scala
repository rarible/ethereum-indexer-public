package com.rarible.protocol.erc20.contract

import com.rarible.contracts.interfaces.weth9.{DepositEvent, WithdrawalEvent}
import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple2Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.response

object WithdrawalEventByLogData {
  import TopicFilter.simple

  val event = Event("Withdrawal", List(AddressType, Uint256Type), UnitType, Tuple2Type(AddressType, Uint256Type))
  val id: Word = Word.apply("0x7fcf532c15f0a6db0bd6d0e038bea71d30d808c7d98cb3bf7268a95bf5081b65")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[WithdrawalEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(WithdrawalEventByLogData(_))

  def apply(log: response.Log): WithdrawalEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val src = decodedData._1
    val wad = decodedData._2
    WithdrawalEvent(log, src, wad)
  }
}
