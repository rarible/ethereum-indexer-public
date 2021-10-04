package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.CancelEvent
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class ExchangeV2CancelDescriptorTest : AbstractExchangeV2Test() {

    @Test
    fun convert() = runBlocking {
        val orderVersion = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )
        orderUpdateService.save(orderVersion)
        val order = orderVersion.toOrderExactFields()

        exchange.cancel(order.forTx()).withSender(userSender1).execute().verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.CANCEL).collectList().awaitFirst()
            assertThat(items).hasSize(1)

            val event = items.first().data as OrderCancel
            assertThat(event.hash).isEqualTo(order.hash)

            val canceledOrder = orderRepository.findById(order.hash)
            assertThat(canceledOrder?.cancelled).isTrue()
            assertThat(canceledOrder?.makeStock).isEqualTo(EthUInt256.ZERO)

            checkActivityWasPublished(order, CancelEvent.id(), OrderActivityCancelListDto::class.java)
        }
    }
}
