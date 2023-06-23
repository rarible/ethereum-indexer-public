package com.rarible.protocol.order.core.service.pool

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.createSudoSwapPoolDataV1
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.data.randomPoolNftDeposit
import com.rarible.protocol.order.core.data.randomPoolSpotPriceUpdate
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

internal class EventPoolReducerTest {
    private val eventPoolReducer = EventPoolReducer()

    @Test
    fun `should reduce onChainAmmOrder event`() = runBlocking<Unit> {
        val init = randomOrder()
        val data = createSudoSwapPoolDataV1()
        val event = randomSellOnChainAmmOrder(data)

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.type).isEqualTo(OrderType.AMM)
        assertThat(reduced.hash).isEqualTo(event.hash)
        assertThat(reduced.maker).isEqualTo(data.poolAddress)
        assertThat(reduced.make).isEqualTo(event.nftAsset())
        assertThat(reduced.take).isEqualTo(event.currencyAsset())
        assertThat(reduced.createdAt).isEqualTo(event.date)
        assertThat(reduced.platform).isEqualTo(event.source.toPlatform())
        assertThat(reduced.data).isEqualTo(event.data.toOrderData())
        assertThat(reduced.takePrice).isNull()
    }

    @Test
    fun `should reduce poll nft out event`() = runBlocking<Unit> {
        val make = randomErc721().copy(value = EthUInt256.ONE)
        val init = createSellOrder().copy(make = make, lastUpdateAt = past())
        val event = randomPoolTargetNftOut().copy(tokenIds = generateIds(7), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.ONE)
        assertThat(reduced.makeStock).isEqualTo(EthUInt256.of(3))
    }

    @Test
    fun `should reduce poll nft out event safely`() = runBlocking<Unit> {
        val make = randomAmmNftAsset()
        val init = createSellOrder().copy(make = make, makeStock = EthUInt256.of(7), lastUpdateAt = past())
        val event = randomPoolTargetNftOut().copy(tokenIds = generateIds(7), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.makeStock).isEqualTo(EthUInt256.ZERO)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should reduce poll nft in event`() = runBlocking<Unit> {
        val make = randomAmmNftAsset()
        val init = createSellOrder().copy(make = make, makeStock = EthUInt256.of(5), lastUpdateAt = past())
        val event = randomPoolTargetNftIn().copy(tokenIds = generateIds(5), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.makeStock).isEqualTo(EthUInt256.TEN)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should reduce poll nft deposit event`() = runBlocking<Unit> {
        val collection = randomAddress()
        val make = randomAmmNftAsset(collection)
        val init = createSellOrder().copy(make = make, makeStock = EthUInt256.of(5))
        val event = randomPoolNftDeposit().copy(tokenIds = generateIds(5), collection = collection, date = past())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(init.make.value)
        assertThat(reduced.makeStock).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should not reduce poll nft in event if deposit wrong collection`() = runBlocking<Unit> {
        val make = randomAmmNftAsset().copy(value = EthUInt256.of(5))
        val init = createSellOrder(createOrderSudoSwapAmmDataV1()).copy(make = make, lastUpdateAt = past())
        val event = randomPoolNftDeposit().copy(tokenIds = generateIds(5), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.of(5))
    }

    @Test
    fun `should reduce poll spot price update event`() = runBlocking<Unit> {
        val init = createSellOrder(createOrderSudoSwapAmmDataV1())
        val event = randomPoolSpotPriceUpdate().copy(newSpotPrice = BigInteger("200000000000000000"), date = now())
        val reduced = eventPoolReducer.reduce(init, event)
        assertThat((reduced.data as OrderSudoSwapAmmDataV1).spotPrice).isEqualTo(event.newSpotPrice)
    }

    @Test
    fun `should reduce poll spot delta update`() = runBlocking<Unit> {
        val init = createSellOrder(createOrderSudoSwapAmmDataV1()).copy(lastUpdateAt = past())
        val event = randomPoolDeltaUpdate().copy(date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat((reduced.data as OrderSudoSwapAmmDataV1).delta).isEqualTo(event.newDelta)
    }

    @Test
    fun `should reduce poll spot fee update`() = runBlocking<Unit> {
        val init = createSellOrder(createOrderSudoSwapAmmDataV1()).copy(lastUpdateAt = past())
        val event = randomPoolFeeUpdate().copy(date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat((reduced.data as OrderSudoSwapAmmDataV1).fee).isEqualTo(event.newFee)
    }

    private fun past(): Instant = Instant.now() - Duration.ofDays(1)

    private fun now(): Instant = Instant.now()

    private fun generateIds(value: Int): List<EthUInt256> {
        return (1..value).map { EthUInt256.of(it) }
    }
}
