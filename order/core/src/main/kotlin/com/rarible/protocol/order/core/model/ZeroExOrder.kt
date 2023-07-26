package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address
import java.math.BigInteger

data class ZeroExOrder(
    val makerAddress: Address,
    val takerAddress: Address,
    val feeRecipientAddress: Address,
    val senderAddress: Address,
    val makerAssetAmount: BigInteger,
    val takerAssetAmount: BigInteger,
    val makerFee: BigInteger,
    val takerFee: BigInteger,
    val expirationTimeSeconds: BigInteger,
    val salt: BigInteger,
    val makerAssetData: Binary,
    val takerAssetData: Binary,
    val makerFeeAssetData: Binary,
    val takerFeeAssetData: Binary,
)
