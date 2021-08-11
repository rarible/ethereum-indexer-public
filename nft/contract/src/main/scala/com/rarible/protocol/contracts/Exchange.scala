package com.rarible.protocol.contracts

import java.math.BigInteger

import scalether.abi.tuple.{Tuple1Type, Tuple2Type, Tuple3Type, Tuple4Type}
import scalether.abi.{AddressType, Uint256Type, Uint8Type}
import scalether.domain.Address

object Exchange {
  private val orderType = Tuple4Type(Tuple4Type(AddressType, Uint256Type, Tuple3Type(AddressType, Uint256Type, Uint8Type), Tuple3Type(AddressType, Uint256Type, Uint8Type)), Uint256Type, Uint256Type, Uint256Type)
  val orderMessageType: Tuple1Type[((Address, BigInteger, (Address, BigInteger, BigInteger), (Address, BigInteger, BigInteger)), BigInteger, BigInteger, BigInteger)] =
    Tuple1Type(orderType)
  val buyerFeeMessageType: Tuple2Type[((Address, BigInteger, (Address, BigInteger, BigInteger), (Address, BigInteger, BigInteger)), BigInteger, BigInteger, BigInteger), BigInteger] =
    Tuple2Type(orderType, Uint256Type)
}
