package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.RaribleMatchedOrders.SimpleOrder
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component

@Component
class RaribleExchangeV2OrderParser {
    fun parseMatchedOrders(input: String): RaribleMatchedOrders {
        val decoded = ExchangeV2.matchOrdersSignature().`in`().decode(Binary.apply(input), 4)

        return RaribleMatchedOrders(
            left = SimpleOrder(
                data = convertOrderData(
                    version = Binary.apply(decoded.value()._1()._8()),
                    data = Binary.apply(decoded.value()._1()._9())
                ),
                salt = EthUInt256.of(decoded.value()._1()._5())
            ),
            right = SimpleOrder(
                data = convertOrderData(
                    version = Binary.apply(decoded.value()._3()._8()),
                    data = Binary.apply(decoded.value()._3()._9())
                ),
                salt = EthUInt256.of(decoded.value()._3()._5())
            )
        )
    }

    fun convertOrderData(version: Binary, data: Binary): OrderData {
        return when (version) {
            OrderDataVersion.RARIBLE_V2_DATA_V1.ethDataType -> {
                val (payouts, originFees) = when {
                    data.slice(0, 32) ==  ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX -> {
                        val decoded = Tuples.orderDataV1Type().decode(data, 0)
                        decoded.value()._1().map { it.toPart() } to decoded.value()._2().map { it.toPart() }
                    }
                    data.slice(0, 32) ==  WRONG_ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX -> {
                        val decodedWrong = Tuples.wrongOrderDataV1Type().decode(data, 0)
                        decodedWrong.value()._1().map { it.toPart() } to decodedWrong.value()._2().map { it.toPart() }
                    }
                    else -> throw IllegalArgumentException("Unsupported data encode (data=$data)")
                }
                OrderRaribleV2DataV1(
                    payouts = payouts,
                    originFees = originFees
                )
            }
            else -> throw IllegalArgumentException("Unsupported order data version $version")
        }
    }

    private companion object {
        val WRONG_ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary = Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000040")
        val ENCODED_ORDER_RARIBLE_V2_DATA_V1_PREFIX: Binary = Binary.apply("0x0000000000000000000000000000000000000000000000000000000000000020")
    }
}
