package com.rarible.protocol.order.core.service.looksrare

import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.looksrare.client.LooksrareClientV2
import com.rarible.looksrare.client.model.LooksrareResult
import com.rarible.looksrare.client.model.v2.LooksrareOrders
import com.rarible.looksrare.client.model.v2.OrdersRequest
import com.rarible.looksrare.client.model.v2.Pagination
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Sort
import com.rarible.looksrare.client.model.v2.Status
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class LooksrareOrderServiceTest {
    @MockK
    private lateinit var looksrareClient: LooksrareClientV2

    @MockK
    private lateinit var metrics: ForeignOrderMetrics

    @SpyK
    private var properties = LooksrareLoadProperties()

    @InjectMockKs
    private lateinit var service: LooksrareOrderService

    @Test
    fun `should get all order in one iteration`() = runBlocking<Unit> {
        val createdAfter = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))

        val order1 = randomLooksrareOrder().copy(createdAt = createdAfter.createdAfter, status = Status.VALID)
        val order2 = randomLooksrareOrder().copy(
            createdAt = createdAfter.createdAfter - Duration.ofHours(2),
            status = Status.VALID
        )

        val result = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        every { metrics.onCallForeignOrderApi(any(), any()) } returns Unit
        coEvery { looksrareClient.getOrders(any()) } returns LooksrareResult.success(result)

        val orders = service.getNextSellOrders(createdAfter)
        assertThat(orders).containsExactly(order1, order2)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match {
                    it.status == Status.VALID &&
                        it.sort == Sort.NEWEST &&
                        it.pagination?.first == 150 &&
                        it.pagination?.cursor == null
                }
            )
        }
    }

    @Test
    fun `should get all order in two iterations`() = runBlocking<Unit> {
        val now = Instant.now()
        val createdAfter = LooksrareV2Cursor(now - Duration.ofSeconds(10))

        val order1 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now)
        val order2 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(1))
        val order3 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(2))
        val order4 = randomLooksrareOrder().copy(status = Status.VALID, createdAt = now - Duration.ofSeconds(11))

        val result1 = LooksrareOrders(success = true, message = "", data = listOf(order1, order2))
        val result2 = LooksrareOrders(success = true, message = "", data = listOf(order3, order4))
        every { metrics.onCallForeignOrderApi(any(), any()) } returns Unit
        coEvery { looksrareClient.getOrders(any()) } returnsMany listOf(
            LooksrareResult.success(result1),
            LooksrareResult.success(result2)
        )

        val orders = service.getNextSellOrders(createdAfter)
        assertThat(orders).containsExactly(order1, order2, order3, order4)

        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == null }
            )
        }
        coVerify(exactly = 1) {
            looksrareClient.getOrders(
                match { it.pagination?.cursor == order2.id }
            )
        }
    }

    @Test
    fun `isActiveOrder order found`() = runBlocking<Unit> {
        val order = randomOrder(token = Address.ONE(), tokenId = EthUInt256(BigInteger.ONE))
        val lastId = randomWord()
        every { metrics.onCallForeignOrderApi(any(), any()) } returns Unit
        coEvery {
            looksrareClient.getOrders(match {
                it == OrdersRequest(
                    collection = Address.ONE(),
                    itemId = "1",
                    quoteType = QuoteType.ASK,
                    status = Status.VALID,
                    sort = Sort.NEWEST,
                    pagination = Pagination(first = properties.loadMaxSize, cursor = null)
                )
            })
        } returns LooksrareResult.success(
            LooksrareOrders(
                success = true,
                message = null,
                data = listOf(
                    randomLooksrareOrder(),
                    randomLooksrareOrder(),
                    randomLooksrareOrder(id = lastId),
                )
            )
        )
        coEvery {
            looksrareClient.getOrders(match {
                it == OrdersRequest(
                    collection = Address.ONE(),
                    itemId = "1",
                    quoteType = QuoteType.ASK,
                    status = Status.VALID,
                    sort = Sort.NEWEST,
                    pagination = Pagination(first = properties.loadMaxSize, cursor = lastId)
                )
            })
        } returns LooksrareResult.success(
            LooksrareOrders(
                success = true,
                message = null,
                data = listOf(
                    randomLooksrareOrder(hash = order.hash),
                )
            )
        )

        val result = service.isActiveOrder(order)

        assertThat(result).isTrue()
    }

    @Test
    fun `isActiveOrder order not found`() = runBlocking<Unit> {
        val order = randomOrder(token = Address.ONE(), tokenId = EthUInt256(BigInteger.ONE))
        val lastId = randomWord()
        every { metrics.onCallForeignOrderApi(any(), any()) } returns Unit
        coEvery {
            looksrareClient.getOrders(match {
                it == OrdersRequest(
                    collection = Address.ONE(),
                    itemId = "1",
                    quoteType = QuoteType.ASK,
                    status = Status.VALID,
                    sort = Sort.NEWEST,
                    pagination = Pagination(first = properties.loadMaxSize, cursor = null)
                )
            })
        } returns LooksrareResult.success(
            LooksrareOrders(
                success = true,
                message = null,
                data = listOf(
                    randomLooksrareOrder(),
                    randomLooksrareOrder(),
                    randomLooksrareOrder(id = lastId),
                )
            )
        )
        coEvery {
            looksrareClient.getOrders(match {
                it == OrdersRequest(
                    collection = Address.ONE(),
                    itemId = "1",
                    quoteType = QuoteType.ASK,
                    status = Status.VALID,
                    sort = Sort.NEWEST,
                    pagination = Pagination(first = properties.loadMaxSize, cursor = lastId)
                )
            })
        } returns LooksrareResult.success(
            LooksrareOrders(
                success = true,
                message = null,
                data = emptyList()
            )
        )

        val result = service.isActiveOrder(order)

        assertThat(result).isFalse()
    }

    @Test
    fun `isActiveOrder order depth exhausted`() = runBlocking<Unit> {
        val order = randomOrder(token = Address.ONE(), tokenId = EthUInt256(BigInteger.ONE))
        every { metrics.onCallForeignOrderApi(any(), any()) } returns Unit
        coEvery {
            looksrareClient.getOrders(any())
        } returns LooksrareResult.success(
            LooksrareOrders(
                success = true,
                message = null,
                data = listOf(
                    randomLooksrareOrder(),
                    randomLooksrareOrder(),
                    randomLooksrareOrder(),
                )
            )
        )

        val result = service.isActiveOrder(order)

        assertThat(result).isTrue()
    }
}
