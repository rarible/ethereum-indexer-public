package com.rarible.protocol.contracts

import io.daonomic.rpc.domain.{Bytes, Word}
import kotlin.text.Charsets
import org.web3j.crypto.Hash
import scalether.abi._
import scalether.abi.array.VarArrayType
import scalether.abi.tuple._

//noinspection TypeAnnotation
object Tuples {
  val eip712DomainHashType =
    Tuple5Type(Bytes32Type, Bytes32Type, Bytes32Type, Uint256Type, AddressType)

  val addressUintType =
    Tuple2Type(AddressType, Uint256Type)

  val assetTypeHashType =
    Tuple3Type(Bytes32Type, Bytes4Type, Bytes32Type)

  val assetHashType =
    Tuple3Type(Bytes32Type, Bytes32Type, Uint256Type)

  val orderKeyHashType =
    Tuple4Type(AddressType, Bytes32Type, Bytes32Type, Uint256Type)

  val raribleAuctionKeyHashType =
    Tuple2Type(AddressType, Uint256Type)

  val orderHashType =
    Tuple10Type(Bytes32Type, AddressType, Bytes32Type, AddressType, Bytes32Type, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, Bytes32Type)

  val legacyOrderHashType =
    Tuple1Type(Tuple4Type(Tuple4Type(AddressType, Uint256Type, Tuple3Type(AddressType, Uint256Type, Uint8Type), Tuple3Type(AddressType, Uint256Type, Uint8Type)), Uint256Type, Uint256Type, Uint256Type))

  val assetTypeType =
    Tuple2Type(Bytes4Type, BytesType)

  val assetType =
    Tuple2Type(assetTypeType, Uint256Type)

  val orderType =
    Tuple9Type(AddressType, assetType, AddressType, assetType, Uint256Type, Uint256Type, Uint256Type, Bytes4Type, BytesType)

  val wrongOrderDataV1Type =
    Tuple2Type(VarArrayType(addressUintType), VarArrayType(addressUintType))

  val orderDataV1Type =
    Tuple1Type(wrongOrderDataV1Type)

  val orderDataLegacyType =
    Tuple1Type(Uint256Type)

  val auctionDataV1Type =
    Tuple1Type(Tuple5Type(VarArrayType(addressUintType), VarArrayType(addressUintType), Uint256Type, Uint256Type, Uint256Type))

  val auctionBidDataV1Type =
    Tuple1Type(Tuple2Type(VarArrayType(addressUintType), VarArrayType(addressUintType)))

  private val lazy721MessageType =
    Tuple5Type(Uint256Type, StringType, VarArrayType(Tuple2Type(AddressType, Uint256Type)), VarArrayType(Tuple2Type(AddressType, Uint256Type)), VarArrayType(BytesType))

  val lazy721Type =
    Tuple2Type(AddressType, lazy721MessageType)

  private val lazy1155MessageType =
    Tuple6Type(Uint256Type, StringType, Uint256Type, VarArrayType(Tuple2Type(AddressType, Uint256Type)), VarArrayType(Tuple2Type(AddressType, Uint256Type)), VarArrayType(BytesType))

  val lazy1155Type =
    Tuple2Type(AddressType, lazy1155MessageType)

  val partHashType =
    Tuple3Type(Bytes32Type, AddressType, Uint256Type)

  val erc721ReplacementPattern =
    Tuple3Type(Bytes32Type, Bytes32Type, Uint256Type)

  val erc1155ReplacementPattern =
    Tuple5Type(Bytes32Type, Bytes32Type, Uint256Type, Uint256Type, BytesType)

  def keccak256(str: String): Word = keccak256(str.getBytes(Charsets.US_ASCII))
  def keccak256(bytes: Bytes): Word = keccak256(bytes.bytes)
  def keccak256(bytes: Array[Byte]): Word = Word(Hash.sha3(bytes))
}
