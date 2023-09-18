package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.contract.MonoPreparedTransaction
import java.time.Instant

@IntegrationTest
@FlowPreview
class ExchangeV2CancelDescriptorIt : AbstractExchangeV2Test() {

    @Test
    fun convertLegacyV2() = runBlocking {
        orderIndexerProperties.exchangeContractAddresses.v2 = legacyExchange.address()
        testCancelOrder {
            legacyExchange.cancel(it.forTx())
        }
    }

    @Test
    fun convertRev2() = runBlocking {
        orderIndexerProperties.exchangeContractAddresses.v2 = exchange.address()
        testCancelOrder {
            exchange.cancel(it.forTx())
        }
    }

    private suspend fun testCancelOrder(cancel: (Order) -> MonoPreparedTransaction<*>) {
        val orderVersion = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = Instant.MAX.epochSecond,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        save(orderVersion)
        val order = orderVersion.toOrderExactFields()

        cancel(order).withSender(userSender1).execute().verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.CANCEL).collectList().awaitFirst()
            assertThat(items).hasSize(1)

            val event = items.first().data as OrderCancel
            assertThat(event.hash).isEqualTo(order.hash)

            val canceledOrder = orderRepository.findById(order.hash)
            assertThat(canceledOrder?.cancelled).isTrue()
            assertThat(canceledOrder?.makeStock).isEqualTo(EthUInt256.ZERO)

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityCancelListDto::class.java) {
                    assertThat(it.hash).isEqualTo(order.hash)
                }
            }
        }
    }
}
