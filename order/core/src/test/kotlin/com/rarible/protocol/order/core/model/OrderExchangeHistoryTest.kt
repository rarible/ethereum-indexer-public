package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOnChainOrder
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.misc.MAPPER
import io.daonomic.rpc.domain.WordFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.AddressFactory
import java.util.stream.Stream

class OrderExchangeHistoryTest {
    @Test
    fun checkNotBid() {
        assertThat(
            OrderCancel(
                hash = WordFactory.create(),
                maker = AddressFactory.create(),
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                date = nowMillis(),
                source = HistorySource.RARIBLE
            ).isBid()
        ).isFalse()
        assertThat(
            OrderCancel(
                hash = WordFactory.create(),
                maker = AddressFactory.create(),
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                take = null,
                date = nowMillis(),
                source = HistorySource.RARIBLE
            ).isBid()
        ).isFalse()
    }

    @Test
    fun checkBid() {
        assertThat(
            OrderCancel(
                hash = WordFactory.create(),
                maker = AddressFactory.create(),
                make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                take = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.ONE), EthUInt256.TEN),
                date = nowMillis(),
                source = HistorySource.RARIBLE
            ).isBid()
        ).isTrue()
    }

    private companion object {
        private val assetTypes = listOf<Pair<OrderExchangeHistory, Class<*>>>(
            createOrderSideMatch() to OrderSideMatch::class.java,
            createOrderCancel() to OrderCancel::class.java,
            createOnChainOrder() to OnChainOrder::class.java
        )

        @JvmStatic
        fun orderExchangeHistoryStream(): Stream<Arguments> = run {
            assetTypes.stream().map { Arguments.of(it.first, it.second) }
        }
    }

    @ParameterizedTest
    @MethodSource("orderExchangeHistoryStream")
    fun `serialize and deserialize - ok`(exchangeHistory: OrderExchangeHistory, exchangeHistoryClass: Class<*>) {
        val json = MAPPER.writeValueAsString(exchangeHistory)
        println(json)
        val deserialized = MAPPER.readValue(json, exchangeHistoryClass)
        assertThat(deserialized).isEqualTo(exchangeHistory)
    }
}
