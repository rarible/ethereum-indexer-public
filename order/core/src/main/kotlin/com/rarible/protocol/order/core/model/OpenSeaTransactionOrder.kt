package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

data class OpenSeaTransactionOrder(
    val exchange: Address,
    val maker: Address,
    val taker: Address,
    val makerRelayerFee: BigInteger,
    val takerRelayerFee: BigInteger,
    val makerProtocolFee: BigInteger,
    val takerProtocolFee: BigInteger,
    val feeRecipient: Address,
    val feeMethod: OpenSeaOrderFeeMethod,
    val side: OpenSeaOrderSide,
    val saleKind: OpenSeaOrderSaleKind,
    val target: Address,
    val howToCall: OpenSeaOrderHowToCall,
    val callData: Binary,
    val replacementPattern: Binary,
    val staticTarget: Address,
    val staticExtraData: Binary,
    val paymentToken: Address,
    val basePrice: BigInteger,
    val extra: BigInteger,
    val listingTime: BigInteger,
    val expirationTime: BigInteger,
    val salt: BigInteger
) {
    val hash: Word
        get() {
            return Order.openSeaV1Hash(
                maker = maker,
                taker = taker,
                nftToken = target,
                paymentToken = paymentToken,
                basePrice = basePrice,
                salt = salt,
                start = listingTime.longValueExact(),
                end = expirationTime.longValueExact(),
                data = OrderOpenSeaV1DataV1(
                    exchange,
                    makerRelayerFee,
                    takerRelayerFee,
                    makerProtocolFee,
                    takerProtocolFee,
                    feeRecipient,
                    feeMethod,
                    side,
                    saleKind,
                    howToCall,
                    callData,
                    replacementPattern,
                    staticTarget,
                    staticExtraData,
                    extra,
                    target
                )
            )
        }
}

enum class OpenSeaOrderFeeMethod(val value: BigInteger) {
    PROTOCOL_FEE(BigInteger.valueOf(0)),
    SPLIT_FEE(BigInteger.valueOf(1))
    ;

    companion object {
        private val methods = values().associateBy { it.value }

        fun fromBigInteger(value: BigInteger): OpenSeaOrderFeeMethod {
            return methods[value] ?: throw IllegalArgumentException("Unexpected value $value")
        }
    }
}

enum class OpenSeaOrderSide(val value: BigInteger) {
    BUY(BigInteger.valueOf(0)),
    SELL(BigInteger.valueOf(1))
    ;

    companion object {
        private val methods = values().associateBy { it.value }

        fun fromBigInteger(value: BigInteger): OpenSeaOrderSide {
            return methods[value] ?: throw IllegalArgumentException("Unexpected value $value")
        }
    }
}

enum class OpenSeaOrderSaleKind(val value: BigInteger) {
    FIXED_PRICE(BigInteger.valueOf(0)),
    DUTCH_AUCTION(BigInteger.valueOf(1))
    ;

    companion object {
        private val methods = values().associateBy { it.value }

        fun fromBigInteger(value: BigInteger): OpenSeaOrderSaleKind {
            return methods[value] ?: throw IllegalArgumentException("Unexpected value $value")
        }
    }
}

enum class OpenSeaOrderHowToCall(val value: BigInteger) {
    CALL(BigInteger.valueOf(0)),
    DELEGATE_CALL(BigInteger.valueOf(1))
    ;

    companion object {
        private val methods = values().associateBy { it.value }

        fun fromBigInteger(value: BigInteger): OpenSeaOrderHowToCall {
            return methods[value] ?: throw IllegalArgumentException("Unexpected value $value")
        }
    }
}
