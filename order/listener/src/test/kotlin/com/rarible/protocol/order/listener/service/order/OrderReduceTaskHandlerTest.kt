package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.toLogEventKey
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@ExperimentalCoroutinesApi
@IntegrationTest
class OrderReduceTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var orderReduceTaskHandlerInitializer: OrderReduceTaskHandlerInitializer

    @Autowired
    private lateinit var onChainOrderVersionInsertionTaskHandlerInitializer: OnChainOrderVersionInsertionTaskHandlerInitializer

    @Test
    internal fun `should insert OrderVersion and reduce on-chain order`() = runBlocking<Unit> {
        val onChainOrder = createOrderVersion().run {
            OnChainOrder(
                maker = maker,
                taker = taker,
                make = make,
                take = take,
                createdAt = createdAt,
                platform = platform,
                orderType = type,
                salt = salt,
                start = start,
                end = end,
                data = data,
                signature = signature,
                hash = hash,
                priceUsd = makePriceUsd ?: takePriceUsd
            )
        }
        val logEvent = LogEvent(
            data = onChainOrder,
            address = randomAddress(),
            topic = Word.apply(RandomUtils.nextBytes(32)),
            transactionHash = Word.apply(RandomUtils.nextBytes(32)),
            index = RandomUtils.nextInt(),
            minorLogIndex = 0,
            status = LogEventStatus.CONFIRMED
        )
        exchangeHistoryRepository.save(logEvent).awaitFirst()

        // Run order-related tasks.
        onChainOrderVersionInsertionTaskHandlerInitializer.init()

        Wait.waitAssert {
            val task = taskRepository.findByTypeAndParam(
                OnChainOrderVersionInsertionTaskHandler.TYPE,
                ""
            ).awaitFirstOrNull()
            assertNotNull(task)
            assertEquals(TaskStatus.COMPLETED, task!!.lastStatus)
        }

        orderReduceTaskHandlerInitializer.init()

        Wait.waitAssert {
            assertTrue(orderVersionRepository.existsByOnChainOrderKey(logEvent.toLogEventKey()).awaitSingle())

            val order = orderRepository.findById(onChainOrder.hash)
            assertNotNull(order)
            assertEquals(onChainOrder.make, order!!.make)
            assertEquals(onChainOrder.take, order.take)
            assertEquals(onChainOrder.maker, order.maker)
            assertEquals(onChainOrder.taker, order.taker)
            assertFalse(order.cancelled)
        }
    }
}
