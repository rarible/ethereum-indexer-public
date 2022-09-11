package com.rarible.protocol.order.core.service.pool

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.createSudoSwapPoolDataV1
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomEth
import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.data.randomPoolNftDeposit
import com.rarible.protocol.order.core.data.randomPoolSpotPriceUpdate
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

internal class EventPoolReducerTest {
    private val contractService = mockk<ContractService>()
    private val normalizer = PriceNormalizer(contractService)
    private val eventPoolReducer = EventPoolReducer(normalizer)

    @Test
    fun `should reduce onChainAmmOrder event`() = runBlocking<Unit> {
        val init = createOrder()
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
        assertThat(reduced.data).isEqualTo(event.data)
        assertThat(reduced.makePrice).isEqualTo(event.priceValue)
        assertThat(reduced.takePrice).isNull()
    }

    @Test
    fun `should reduce poll nft out event`() = runBlocking<Unit> {
        val amke = randomErc721().copy(value = EthUInt256.of(10))
        val init = createSellOrder().copy(make = amke, lastUpdateAt = past())
        val event = randomPoolTargetNftOut().copy(tokenIds = generateIds(7), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.of(3))
    }

    @Test
    fun `should reduce poll nft out event safely`() = runBlocking<Unit> {
        val amke = randomErc721().copy(value = EthUInt256.of(5))
        val init = createSellOrder().copy(make = amke, lastUpdateAt = past())
        val event = randomPoolTargetNftOut().copy(tokenIds = generateIds(7), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should reduce poll nft in event`() = runBlocking<Unit> {
        val amke = randomErc721().copy(value = EthUInt256.of(5))
        val init = createSellOrder().copy(make = amke, lastUpdateAt = past())
        val event = randomPoolTargetNftIn().copy(tokenIds = generateIds(5), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should reduce poll nft deposit event`() = runBlocking<Unit> {
        val collection = randomAddress()
        val amke = randomAmmNftAsset(collection).copy(value = EthUInt256.of(5))
        val init = createSellOrder(createOrderSudoSwapAmmDataV1()).copy(make = amke, lastUpdateAt = now())
        val event = randomPoolNftDeposit().copy(tokenIds = generateIds(5), collection = collection, date = past())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.make.value).isEqualTo(EthUInt256.TEN)
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
        val init = createSellOrder().copy(makePrice = BigDecimal.valueOf(0.1), take = randomEth(), lastUpdateAt = past())
        val event = randomPoolSpotPriceUpdate().copy(newSpotPrice = BigInteger("200000000000000000"), date = now())

        val reduced = eventPoolReducer.reduce(init, event)
        assertThat(reduced.makePrice).isEqualTo(BigDecimal("0.200000000000000000"))
        assertThat(reduced.takePrice).isNull()
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