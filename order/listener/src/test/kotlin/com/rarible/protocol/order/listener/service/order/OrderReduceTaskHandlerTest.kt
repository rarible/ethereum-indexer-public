package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.toLogEventKey
import com.rarible.protocol.order.core.model.toOnChainOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
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
    private lateinit var taskService: TaskService

    @Test
    internal fun `should insert OrderVersion and reduce on-chain order`() = runBlocking<Unit> {
        val onChainOrder = createOrderVersion().toOnChainOrder()
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

        taskService.runTask(OrderReduceTaskHandler.ORDER_REDUCE, "")

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
