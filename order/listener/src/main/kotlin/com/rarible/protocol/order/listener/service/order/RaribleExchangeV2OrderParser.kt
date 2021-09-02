package com.rarible.protocol.order.listener.service.order

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
                data = convert(
                    version = Binary.apply(decoded.value()._1()._8()),
                    data = Binary.apply(decoded.value()._1()._9())
                )
            ),
            right = SimpleOrder(
                data = convert(
                    version = Binary.apply(decoded.value()._3()._8()),
                    data = Binary.apply(decoded.value()._3()._9())
                )
            )
        )
    }

    private fun convert(version: Binary, data: Binary): OrderData {
        return when (version) {
            OrderDataVersion.RARIBLE_V2_DATA_V1.ethDataType -> {
                val decoded = Tuples.orderDataV1Type().decode(data, 0)
                OrderRaribleV2DataV1(
                    payouts = decoded.value()._1().map { it.toPart() },
                    originFees = decoded.value()._1().map { it.toPart() }
                )
            }
            else -> throw IllegalArgumentException("Unsupported order data version $version")
        }
    }
}
