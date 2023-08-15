package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.looksrare.LooksrareService
import com.rarible.protocol.order.listener.data.randomLooksrareCancelListEvent
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class LooksrareCancelListEventLoaderTest {
    @InjectMockKs
    private lateinit var looksrareCancelListEventLoader: LooksrareCancelListEventLoader

    @MockK
    private lateinit var looksrareService: LooksrareService

    @MockK
    private lateinit var orderStateRepository: OrderStateRepository

    @MockK
    private lateinit var orderUpdateService: OrderUpdateService

    @MockK
    private lateinit var metrics: ForeignOrderMetrics

    @SpyK
    private var properties = LooksrareLoadProperties()

    @Test
    fun load() = runBlocking<Unit> {
        val initialCursor = LooksrareV2Cursor(
            createdAfter = Instant.ofEpochSecond(1),
            nextId = "1",
        )
        val onchainEvent =
            randomLooksrareCancelListEvent().copy(hash = randomWord(), createdAt = Instant.ofEpochSecond(40))
        val offchainNoOrderEvent =
            randomLooksrareCancelListEvent().copy(order = null, createdAt = Instant.ofEpochSecond(30))
        val offchainEventAlreadyApplied = randomLooksrareCancelListEvent().copy(createdAt = Instant.ofEpochSecond(20))
        val offchainEvent = randomLooksrareCancelListEvent().copy(createdAt = Instant.ofEpochSecond(10))
        coEvery { looksrareService.getNextCancelListEvents(initialCursor) } returns listOf(
            onchainEvent,
            offchainNoOrderEvent,
            offchainEventAlreadyApplied,
            offchainEvent
        )
        coEvery { orderStateRepository.getById(offchainEventAlreadyApplied.order!!.hash) } returns
            OrderState(id = offchainEventAlreadyApplied.order!!.hash, createdAt = nowMillis(), canceled = true)
        coEvery { orderStateRepository.getById(Word.apply(offchainEvent.order!!.hash)) } returns null
        coEvery {
            orderStateRepository.save(match {
                it.id == offchainEvent.order!!.hash
            })
        } returns OrderState(id = offchainEvent.order!!.hash, canceled = true)
        coEvery { orderUpdateService.update(offchainEvent.order!!.hash, any()) } returns randomOrder()
        coEvery { metrics.onOrderEventHandled(Platform.LOOKSRARE, "cancel_offchain") } returns Unit
        coEvery { metrics.onOrderReceived(Platform.LOOKSRARE, any(), "order_event") } returns Unit

        val result = looksrareCancelListEventLoader.load(initialCursor)

        assertThat(result).isEqualTo(
            Result(
                cursor = LooksrareV2Cursor(
                    maxSeenCreated = onchainEvent.createdAt,
                    nextId = offchainEvent.id,
                    createdAfter = initialCursor.createdAfter,
                ),
                saved = 1,
            )
        )
    }
}
