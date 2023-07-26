package com.rarible.protocol.order.listener.service.order

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.task.TaskService
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toLogEventKey
import com.rarible.protocol.order.core.model.toOnChainOrder
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class OrderReduceTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Test
    internal fun `should insert OrderVersion and reduce on-chain order`() = runBlocking<Unit> {
        val onChainOrder = createOrderVersion().toOnChainOrder()
        val logEvent = ReversedEthereumLogRecord(
            id = ObjectId().toHexString(),
            data = onChainOrder,
            address = randomAddress(),
            topic = Word.apply(RandomUtils.nextBytes(32)),
            transactionHash = randomWord(),
            index = RandomUtils.nextInt(),
            minorLogIndex = 0,
            status = EthereumBlockStatus.CONFIRMED
        )
        exchangeHistoryRepository.save(logEvent).awaitFirst()

        taskService.runTask(OrderReduceTaskHandler.ORDER_REDUCE, Platform.RARIBLE.name)

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
