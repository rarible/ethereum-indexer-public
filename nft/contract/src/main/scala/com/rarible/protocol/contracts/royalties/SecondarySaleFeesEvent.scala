package com.rarible.protocol.contracts.royalties

import java.math.BigInteger

import io.daonomic.rpc.domain._
import scalether.abi._
import scalether.abi.array._
import scalether.abi.tuple._
import scalether.domain._
import scalether.domain.request._

import scala.language.higherKinds

case class SecondarySaleFeesEvent(log: response.Log, tokenId: BigInteger, recipients: Array[Address], bps: Array[BigInteger])

object SecondarySaleFeesEvent {
  import TopicFilter.simple

  val event: Event[UnitType.type, (BigInteger, Array[Address], Array[BigInteger])] =
    Event("SecondarySaleFees", List(Uint256Type, VarArrayType(AddressType), VarArrayType(Uint256Type)), UnitType, Tuple3Type(Uint256Type, VarArrayType(AddressType), VarArrayType(Uint256Type)))
  val id: Word = Word.apply("0x99aba1d63749cfd5ad1afda7c4663840924d54eb5f005bbbeadedc6ec13674b2")

  def filter(): LogFilter =
    LogFilter(topics = List(simple(id)))

  def apply(receipt: scalether.domain.response.TransactionReceipt): List[SecondarySaleFeesEvent] =
    receipt.logs
      .filter(_.topics.head == id)
      .map(SecondarySaleFeesEvent(_))

  def apply(log: response.Log): SecondarySaleFeesEvent = {
    assert(log.topics.head == id)

    val decodedData = event.decode(log.data)
    val tokenId = decodedData._1
    val recipients = decodedData._2
    val bps = decodedData._3
    SecondarySaleFeesEvent(log, tokenId, recipients, bps)
  }
}