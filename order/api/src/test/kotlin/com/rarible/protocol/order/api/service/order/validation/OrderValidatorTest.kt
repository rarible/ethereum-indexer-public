package com.rarible.protocol.order.api.service.order.validation

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class OrderValidatorTest {
    private val orderVersionValidators1 = mockk<OrderVersionValidator>()
    private val orderVersionValidators2 = mockk<OrderVersionValidator>()
    private val orderPatchValidator1 = mockk<OrderPatchValidator>()
    private val orderPatchValidator2 = mockk<OrderPatchValidator>()
    private val orderValidator = OrderValidator(
        listOf(orderVersionValidators1, orderVersionValidators2),
        listOf(orderPatchValidator1, orderPatchValidator2),
    )

    @Test
    fun `should call all order version validators`() = runBlocking<Unit> {
        val orderVersion = mockk<OrderVersion>()
        coEvery { orderVersionValidators1.validate(orderVersion) } returns Unit
        coEvery { orderVersionValidators2.validate(orderVersion) } returns Unit
        orderValidator.validate(orderVersion)
        coVerify(exactly = 1) { orderVersionValidators1.validate(orderVersion) }
        coVerify(exactly = 1) { orderVersionValidators2.validate(orderVersion) }
    }

    @Test
    fun `should call all order patch validators`() = runBlocking<Unit> {
        val orderVersion = mockk<OrderVersion>()
        val order = mockk<Order>()
        coEvery { orderPatchValidator1.validate(order, orderVersion) } returns Unit
        coEvery { orderPatchValidator2.validate(order, orderVersion) } returns Unit
        orderValidator.validate(order, orderVersion)
        coVerify(exactly = 1) { orderPatchValidator1.validate(order, orderVersion) }
        coVerify(exactly = 1) { orderPatchValidator2.validate(order, orderVersion) }
    }
}
