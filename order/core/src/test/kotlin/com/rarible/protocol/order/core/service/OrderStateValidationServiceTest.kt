package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.metric.OrderValidationMetricsImpl
import com.rarible.protocol.order.core.validator.CompositeOrderValidator
import com.rarible.protocol.order.core.validator.OrderValidator
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OrderStateValidationServiceTest {

    private lateinit var orderStateValidationService: OrderValidator

    @MockK
    private lateinit var validator1: OrderValidator

    @MockK
    private lateinit var validator2: OrderValidator

    @MockK
    private lateinit var metrics: OrderValidationMetricsImpl

    @BeforeEach
    fun before() {
        clearMocks(metrics, validator1, validator2)
        coEvery { metrics.onOrderValidationSuccess(any(), any()) } returns Unit
        coEvery { metrics.onOrderValidationFail(any(), any()) } returns Unit
        every { validator1.type } returns "validator1"
        every { validator2.type } returns "validator2"
        orderStateValidationService = CompositeOrderValidator(
            validators = listOf(validator1, validator2),
            orderValidationMetrics = metrics,
        )
    }

    @Test
    fun `validate state - ok`() = runBlocking<Unit> {
        val order = randomOrder()
        every { validator1.supportsValidation(order) } returns true
        every { validator2.supportsValidation(order) } returns true
        coEvery { validator1.validate(order) } returns Unit
        coEvery { validator2.validate(order) } returns Unit

        orderStateValidationService.validate(order)

        coVerify {
            validator1.validate(order)
            validator2.validate(order)
        }
    }

    @Test
    fun `validate state - failed`() = runBlocking<Unit> {
        val order = randomOrder()
        every { validator1.supportsValidation(order) } returns true
        every { validator2.supportsValidation(order) } returns true
        coEvery { validator1.validate(order) } throws OrderDataException("error")
        coEvery { validator2.validate(order) } returns Unit

        assertThrows<OrderDataException> { orderStateValidationService.validate(order) }

        coVerify(exactly = 1) { validator1.validate(order) }
        coVerify(exactly = 0) { validator2.validate(order) }
    }

    @Test
    fun `validate state - ok, skipped not supported`() = runBlocking<Unit> {
        val order = randomOrder()
        every { validator1.supportsValidation(order) } returns false
        every { validator2.supportsValidation(order) } returns true
        coEvery { validator1.validate(order) } throws OrderDataException("error")
        coEvery { validator2.validate(order) } returns Unit

        orderStateValidationService.validate(order)

        coVerify(exactly = 0) { validator1.validate(order) }
        coVerify(exactly = 1) { validator2.validate(order) }
    }
}
