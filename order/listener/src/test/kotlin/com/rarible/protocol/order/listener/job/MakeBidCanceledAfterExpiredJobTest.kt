package com.rarible.protocol.order.listener.job

import com.rarible.core.test.ext.KafkaTest
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.listener.data.createOrderBid
import com.rarible.protocol.order.listener.integration.IntegrationTest
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@IntegrationTest
@KafkaTest
class MakeBidCanceledAfterExpiredJobTest {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoOperations

    @Autowired
    private lateinit var reduceService: OrderReduceService

    private val bids = listOf(
        createOrderBid().copy(lastUpdateAt = Instant.now() - Duration.ofDays(58)), // not expired
        createOrderBid().copy(lastUpdateAt = Instant.now() - Duration.ofDays(31)), // not expired
        createOrderBid().copy(lastUpdateAt = Instant.now() - Duration.ofDays(60)), // expired
        createOrderBid().copy(lastUpdateAt = Instant.now() - Duration.ofDays(64)), // expired
        createOrderBid().copy(lastUpdateAt = Instant.now() - Duration.ofDays(59)), // not expired
    )

    @BeforeEach
    internal fun setUp() {
        mongoTemplate.dropCollection("order").block()
        mongoTemplate.dropCollection("order_version").block()
        bids.forEach {
            runBlocking {
                orderRepository.save(it)
            }
        }

    }

    @Test
    internal fun `should cancel expired bids`() {
        runBlocking {
            val expiredHashes = orderRepository.findAllLiveBidsHashesLastUpdatedBefore(Instant.now() - Duration.ofDays(60)).toList()

            assertThat(expiredHashes).hasSize(2)

            expiredHashes.forEach {
                reduceService.update(it).asFlow().collect {order ->
                    assertThat(order.cancelled).isTrue
                }
            }

            val notExpired = bids.map { it.hash }.filter { it !in expiredHashes }

            assertThat(notExpired).hasSize(3)

            orderRepository.findAll(notExpired).collect {order ->
                assertThat(order.cancelled).isFalse
            }
        }
    }
}
