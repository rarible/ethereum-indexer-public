package com.rarible.protocol.contracts.erc1155

import java.math.BigInteger

import io.daonomic.rpc.domain.Word
import com.rarible.contracts.erc1155.TransferSingleEvent
import scalether.abi.tuple.{Tuple5Type, UnitType}
import scalether.abi.{AddressType, Event, Uint256Type}
import scalether.domain.request.{LogFilter, TopicFilter}
import scalether.domain.{Address, response}

object TransferSingleEventTopics1 {
  import TopicFilter.simple

  val event: Event[UnitType.type, (Address, Address, Address, BigInteger, BigInteger)] =
    Event("TransferSingle", List(AddressType, AddressType, AddressType, Uint256Type, Uint256Type), UnitType, Tuple5Type(AddressType, AddressType, AddressType, Uint256Type, Uint256Type))
  val id: Word = Word.apply("0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[TransferSingleEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(TransferSingleEventTopics1(_))

  def apply(log: response.Log): TransferSingleEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val _operator = decodedData._1
    val _from = decodedData._2
    val _to = decodedData._3
    val _id = decodedData._4
    val _value = decodedData._5
    TransferSingleEvent(log, _operator, _from, _to, _id, _value)
  }
}