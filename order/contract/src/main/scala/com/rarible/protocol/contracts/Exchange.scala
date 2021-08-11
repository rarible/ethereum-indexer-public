package com.rarible.protocol.contracts

import scalether.abi.tuple.{Tuple1Type, Tuple2Type, Tuple3Type, Tuple4Type}
import scalether.abi.{AddressType, Uint256Type, Uint8Type}

//noinspection TypeAnnotation
object Exchange {
  private val orderType = Tuple4Type(Tuple4Type(AddressType, Uint256Type, Tuple3Type(AddressType, Uint256Type, Uint8Type), Tuple3Type(AddressType, Uint256Type, Uint8Type)), Uint256Type, Uint256Type, Uint256Type)
  val orderMessageType = Tuple1Type(orderType)
  val buyerFeeMessageType = Tuple2Type(orderType, Uint256Type)
}
