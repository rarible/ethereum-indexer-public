package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.OpenSeaOrderItems
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.OrdersRequest
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

internal class OpenSeaOrderServiceTestUt {
    private val openSeaClient: OpenSeaClient = mockk()
    private val properties =  OrderListenerProperties()
    private val openSeaOrderService = OpenSeaOrderServiceImpl(openSeaClient, properties)

    private companion object {
        @JvmStatic
        fun arguments(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    1,
                    5,
                    Duration.ofSeconds(1),
                    setOf(
                        1L to 2L,
                        2L to 3L,
                        3L to 4L,
                        4L to 5L
                    )
                ),
                Arguments.of(
                    0,
                    2,
                    Duration.ofSeconds(1),
                    setOf(
                        0L to 1L,
                        1L to 2L
                    )
                ),
                Arguments.of(
                    0,
                    3,
                    Duration.ofSeconds(1),
                    setOf(
                        0L to 1L,
                        1L to 2L,
                        2L to 3L
                    )
                ),
                Arguments.of(
                    2,
                    19,
                    Duration.ofSeconds(30),
                    setOf(
                        2L to 19L
                    )
                ),
                Arguments.of(
                    1,
                    34,
                    Duration.ofSeconds(30),
                    setOf(
                        1L to 31L,
                        31L to 34L
                    )
                ),
                Arguments.of(
                    1,
                    34,
                    Duration.ofSeconds(15),
                    setOf(
                        1L to 16L,
                        16L to 31L,
                        31L to 34L
                    )
                ),
                Arguments.of(
                    0,
                    14,
                    Duration.ofSeconds(15),
                    setOf(
                        0L to 14L
                    )
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun `should load batch correctly`(
        listedAfter: Long,
        listenerBefore: Long,
        loadPeriod: Duration,
        intervals: Set<Pair<Long, Long>>
    ) = runBlocking<Unit> {
        val checkedIntervals = intervals.toMutableSet()

        coEvery { openSeaClient.getOrders(any()) } answers {
            val request = it.invocation.args.first() as OrdersRequest
            val interval = request.listedAfter!!.epochSecond to request.listedBefore!!.epochSecond
            assertThat(checkedIntervals.remove(interval)).isTrue()

            OperationResult.Success(
                OpenSeaOrderItems(
                    count = 1,
                    orders = emptyList()
                )
            )
        }
        openSeaOrderService.getNextOrdersBatch(listedAfter, listenerBefore, loadPeriod, "")
        assertThat(checkedIntervals.isEmpty()).isTrue
        coVerify (exactly = intervals.size) { openSeaClient.getOrders(any()) }
    }
}
