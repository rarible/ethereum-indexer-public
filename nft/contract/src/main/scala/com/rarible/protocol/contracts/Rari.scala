package com.rarible.protocol.contracts

import java.math.BigInteger

import io.daonomic.rpc.domain.Binary
import scalether.abi.array.VarArrayType
import scalether.abi.tuple.Tuple2Type
import scalether.abi.{AddressType, Uint256Type}
import scalether.domain
import scalether.domain.Address

object Rari {
  val arrayType: VarArrayType[(Address, BigInteger)] =
    VarArrayType(Tuple2Type(AddressType, Uint256Type))

  def main(args: Array[String]): Unit = {
    println(
      Binary(arrayType.encode(Array(
        (domain.Address("821aea9a577a9b44299b9c15c88cf3087f3b5544"), new BigInteger("500")),
        (domain.Address("c5fdf4076b8f3a5357c5e395ab970b5b54098fef"), new BigInteger("100")),
        (domain.Address("c5fdf4076b8f3a5357c5e395ab970b5b54098fef"), new BigInteger("100"))
      )).bytes)
    )
  }
}
