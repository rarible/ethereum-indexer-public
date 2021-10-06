package com.rarible.protocol.erc20.contract

import com.rarible.contracts.erc20.ApprovalEvent
import io.daonomic.rpc.domain.Word
import scalether.abi.tuple.{Tuple3Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type}
import scalether.domain.request.LogFilter
import scalether.domain.response

object ApprovalEventByLogData {

  import scalether.domain.request.TopicFilter.simple

  val event = Event("Approval", List(AddressType, AddressType, Uint256Type), UnitType, Tuple3Type(AddressType, AddressType, Uint256Type))
  val id: Word = Word.apply("0x8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[ApprovalEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(ApprovalEventByLogData(_))

  def apply(log: response.Log): ApprovalEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val owner = decodedData._1
    val spender = decodedData._2
    val value = decodedData._3
    ApprovalEvent(log, owner, spender, value)
  }
}
