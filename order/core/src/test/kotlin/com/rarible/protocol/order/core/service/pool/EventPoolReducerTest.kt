package com.rarible.protocol.order.core.service.pool

import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EventPoolReducerTest {
    private val contractService = mockk<ContractService>()
    private val normalizer = PriceNormalizer(contractService)
    private val eventPoolReducer = EventPoolReducer(normalizer)

    @Test
    fun `should reduce onChainAmmOrder event`() = runBlocking<Unit> {
        val init = createOrder()
        val event = randomSellOnChainAmmOrder()

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.hash).isEqualTo(event.hash)
        assertThat(reduced.maker).isEqualTo(event.maker)
        assertThat(reduced.make).isEqualTo(event.make)
        assertThat(reduced.take).isEqualTo(event.take)
        assertThat(reduced.createdAt).isEqualTo(event.date)
        assertThat(reduced.lastUpdateAt).isEqualTo(event.date)
        assertThat(reduced.platform).isEqualTo(event.source.toPlatform())
        assertThat(reduced.data).isEqualTo(event.data)
        assertThat(reduced.makePrice).isEqualTo(event.priceValue)
        assertThat(reduced.takePrice).isNull()
    }
}