package com.rarible.protocol.order.listener.job

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.ReservoirAsksEventFetchState
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.listener.configuration.ReservoirProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.reservoir.client.ReservoirClient
import com.rarible.reservoir.client.model.ReservoirResult
import com.rarible.reservoir.client.model.common.Amount
import com.rarible.reservoir.client.model.common.Currency
import com.rarible.reservoir.client.model.common.Price
import com.rarible.reservoir.client.model.v3.EventInfo
import com.rarible.reservoir.client.model.v3.EventKind
import com.rarible.reservoir.client.model.v3.ReservoirOrder
import com.rarible.reservoir.client.model.v3.ReservoirOrderEvent
import com.rarible.reservoir.client.model.v3.ReservoirOrderEvents
import io.daonomic.rpc.domain.Word
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.AddressFactory
import java.time.Duration
import java.time.Instant
import com.rarible.reservoir.client.model.common.OrderStatus as ReservoirOrderStatus

@ExtendWith(MockKExtension::class)
internal class ReservoirOrdersReconciliationWorkerTest {
    @InjectMockKs
    private lateinit var reservoirOrdersReconciliationWorker: ReservoirOrdersReconciliationWorker

    @MockK
    private lateinit var reservoirClient: ReservoirClient

    @MockK
    private lateinit var aggregatorStateRepository: AggregatorStateRepository

    @MockK
    private lateinit var orderCancelService: OrderCancelService

    @SpyK
    private var properties: ReservoirProperties = ReservoirProperties(
        enabled = true,
        cancelEnabled = true,
        size = 3,
    )

    @MockK
    private lateinit var orderRepository: OrderRepository

    @MockK
    private lateinit var foreignOrderMetrics: ForeignOrderMetrics

    @SpyK
    private var meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Test
    fun `order canceled`() = runBlocking<Unit> {
        val cursor = randomString()
        val cursor2 = randomString()
        coEvery { aggregatorStateRepository.getReservoirAsksEventState() } returns ReservoirAsksEventFetchState(
            cursor = cursor
        )
        val canceledOrderId = Word.apply(randomWord())
        val inconsistentOrderId = Word.apply(randomWord())
        val canceledOrder = createReservoirOrderEvent(id = canceledOrderId)
        val inconsistentOrder = createReservoirOrderEvent(id = inconsistentOrderId)
        coEvery {
            reservoirClient.getAskEventsV3(any())
        } returns ReservoirResult.success(
            ReservoirOrderEvents(
                events = listOf(
                    canceledOrder,
                    inconsistentOrder,
                    createReservoirOrderEvent(eventKind = EventKind.NEW_ORDER, status = ReservoirOrderStatus.ACTIVE),
                ),
                continuation = cursor2,
            )
        )
        coEvery { orderRepository.findById(canceledOrderId) } returns randomOrder().copy(cancelled = true)
        coEvery { orderRepository.findById(inconsistentOrderId) } returns randomOrder().copy(
            platform = Platform.X2Y2
        )
        coEvery {
            orderCancelService.cancelOrder(
                id = eq(inconsistentOrderId),
                eventTimeMarksDto = any()
            )
        } returns createSellOrder()
        coEvery { foreignOrderMetrics.onOrderInconsistency(platform = Platform.X2Y2, status = "CANCELED") } returns Unit
        coEvery { aggregatorStateRepository.save(ReservoirAsksEventFetchState(cursor = cursor2)) } returns Unit
        val timer = mockk<Timer>()
        coEvery { meterRegistry.timer("reservoir_reconciliation_delay") } returns timer
        coEvery { timer.record(any<Duration>()) } returns Unit

        reservoirOrdersReconciliationWorker.handle()

        coVerify {
            orderCancelService.cancelOrder(id = eq(inconsistentOrderId), eventTimeMarksDto = any())
            foreignOrderMetrics.onOrderInconsistency(platform = Platform.X2Y2, status = "CANCELED")
            aggregatorStateRepository.save(ReservoirAsksEventFetchState(cursor = cursor2))
            timer.record(any<Duration>())
        }
    }

    private fun createReservoirOrderEvent(
        id: Word = Word.apply(randomWord()),
        eventKind: EventKind = EventKind.CANCEL,
        status: ReservoirOrderStatus = ReservoirOrderStatus.CANCELED,
    ): ReservoirOrderEvent = ReservoirOrderEvent(
        event = EventInfo(
            createdAt = Instant.now(),
            id = randomBigInt(),
            txHash = null,
            txTimestamp = null,
            kind = eventKind,
        ),
        order = ReservoirOrder(
            contract = AddressFactory.create(),
            id = id.toString(),
            isDynamic = false,
            kind = "SEAPORT_V1_5",
            maker = AddressFactory.create(),
            price = Price(
                currency = Currency(
                    contract = AddressFactory.create(),
                    decimals = 10,
                ),
                amount = Amount(
                    raw = randomBigInt(),
                    decimal = 10.toBigDecimal(),
                    usd = randomBigDecimal(),
                    native = randomBigDecimal(),
                ),
                netAmount = null,
            ),
            quantityRemaining = randomBigInt(),
            source = "OPEN_SEA",
            validFrom = Instant.now(),
            validUntil = Instant.now(),
            status = status,
        )
    )
}
