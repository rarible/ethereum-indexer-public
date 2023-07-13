package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskService
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class OrderUpdateApprovalTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var properties: OrderListenerProperties

    @Test
    internal fun `should update orders approval`() = runBlocking<Unit> {
        properties.fixApproval = true
        val (_, userSender, _) = newSender()
        val token = createToken(userSender)

        val taskParam = AbstractOrderUpdateStatusTaskHandler.TaskParam(
            status = OrderStatus.ACTIVE,
            platform = Platform.RARIBLE,
            listedAfter = (Instant.now() - Duration.ofNanos(1)).epochSecond
        )
        val param = AbstractOrderUpdateStatusTaskHandler.objectMapper.writeValueAsString(taskParam)
        val hash = Word.apply(randomWord())

        val orderVersion1 = createOrderVersion().copy(hash = hash, make = randomErc721(token.address()), approved = false)
        val orderVersion2 = createOrderVersion().copy(hash = hash, make = randomErc721(token.address()), approved = true)
        listOf(orderVersion1, orderVersion2).forEach {
            orderVersionRepository.save(it).awaitSingle()
        }
        orderUpdateService.update(hash, orderStubEventMarks())
        val updated = orderRepository.findById(hash)
        assertThat(updated?.approved).isEqualTo(true)

        taskService.runTask(OrderUpdateApprovalTaskHandler.UPDATE_ORDER_APPROVAL, param)

        Wait.waitAssert {
            val fixed = orderRepository.findById(hash)
            assertThat(fixed?.approved).isEqualTo(false)
            assertThat(fixed?.status).isEqualTo(OrderStatus.INACTIVE)
        }
    }

    @Test
    internal fun `should cancel x2y2 on approve update`() = runBlocking<Unit> {
        properties.fixApproval = true
        val (_, userSender, _) = newSender()
        val token = createToken(userSender)

        val taskParam = AbstractOrderUpdateStatusTaskHandler.TaskParam(
            status = OrderStatus.ACTIVE,
            platform = Platform.X2Y2,
            listedAfter = (Instant.now() - Duration.ofNanos(1)).epochSecond
        )
        val param = AbstractOrderUpdateStatusTaskHandler.objectMapper.writeValueAsString(taskParam)
        val hash = Word.apply(randomWord())

        val orderVersion1 = createOrderVersion().copy(hash = hash, make = randomErc721(token.address()), approved = false, platform = Platform.X2Y2)
        val orderVersion2 = createOrderVersion().copy(hash = hash, make = randomErc721(token.address()), approved = true, platform = Platform.X2Y2)
        listOf(orderVersion1, orderVersion2).forEach {
            orderVersionRepository.save(it).awaitSingle()
        }
        orderUpdateService.update(hash, orderStubEventMarks())
        val updated = orderRepository.findById(hash)
        assertThat(updated?.approved).isEqualTo(true)

        taskService.runTask(OrderUpdateApprovalTaskHandler.UPDATE_ORDER_APPROVAL, param)

        Wait.waitAssert {
            val fixed = orderRepository.findById(hash)
            assertThat(fixed?.approved).isEqualTo(false)
            assertThat(fixed?.status).isEqualTo(OrderStatus.CANCELLED)
        }
    }
}
