package com.rarible.protocol.order.listener.job

import com.rarible.core.test.ext.KafkaTest
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import com.rarible.protocol.order.listener.data.createBidOrderVersion
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

@IntegrationTest
@KafkaTest
class MakeBidCanceledAfterExpiredJobTest {

    @Autowired
    private lateinit var orderVersionRepository: OrderVersionRepository

    @Autowired
    private lateinit var reduceService: OrderReduceService

    @Autowired
    private lateinit var properties: OrderIndexerProperties

    @Autowired
    private lateinit var assetBalanceProvider: AssetBalanceProvider

    @Test
    internal fun `cancel expired`() = runBlocking<Unit> {
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns null

        val now = Instant.now()
        val bidExpirePeriod = properties.raribleOrderExpiration.bidExpirePeriod
        val expiredBids = run {
            listOf(
                createBidOrderVersion().copy(createdAt = now - bidExpirePeriod),
                createBidOrderVersion().copy(createdAt = now - bidExpirePeriod - Duration.ofHours(1)),
                createBidOrderVersion().copy(createdAt = now, end = (now - Duration.ofHours(1)).epochSecond),
            )
        }
        expiredBids.forEach {
            val hash = orderVersionRepository.save(it).awaitFirst().hash
            val updatedOrder = reduceService.update(hash).collectList().awaitFirst().single()
            assertThat(updatedOrder.cancelled).isTrue
            assertThat(updatedOrder.status).isEqualTo(OrderStatus.CANCELLED)
        }
    }

    @Test
    internal fun `not cancel`() = runBlocking<Unit> {
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns null

        val now = Instant.now()
        val bidExpirePeriod = properties.raribleOrderExpiration.bidExpirePeriod
        val notExpiredBids = run {
            listOf(
                createBidOrderVersion().copy(createdAt = now),
                createBidOrderVersion().copy(createdAt = now - bidExpirePeriod + Duration.ofHours(1)),
                createBidOrderVersion().copy(createdAt = now, end = (now + Duration.ofHours(1)).epochSecond),
            )
        }
        notExpiredBids.forEach {
            val hash = orderVersionRepository.save(it).awaitFirst().hash
            val updatedOrder = reduceService.update(hash).collectList().awaitFirst().single()
            assertThat(updatedOrder.cancelled).isFalse
            assertThat(updatedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }
}
