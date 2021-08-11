package com.rarible.protocol.order.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.WordFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

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

}
