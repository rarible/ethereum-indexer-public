package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.metric.OrderValidationMetrics
import com.rarible.protocol.order.core.model.Order
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.RuntimeException

@ExtendWith(MockKExtension::class)
internal class CompositeOrderValidatorTest {
    private lateinit var compositeOrderValidator: CompositeOrderValidator

    @MockK
    private lateinit var validator1: OrderValidator

    @MockK
    private lateinit var validator2: OrderValidator

    @MockK
    private lateinit var orderValidationMetrics: OrderValidationMetrics

    private lateinit var order: Order

    @BeforeEach
    fun before() {
        compositeOrderValidator = CompositeOrderValidator(
            type = "test",
            validators = listOf(validator1, validator2),
            orderValidationMetrics = orderValidationMetrics,
        )
        order = randomOrder()
    }

    @Test
    fun `all success`() = runBlocking<Unit> {
        coEvery { validator1.type } returns "type1"
        coEvery { validator2.type } returns "type2"

        coEvery { validator1.validate(order) } returns Unit
        coEvery { validator2.validate(order) } returns Unit

        coEvery { validator1.supportsValidation(order) } returns true
        coEvery { validator2.supportsValidation(order) } returns true

        coEvery {
            orderValidationMetrics.onOrderValidationSuccess(
                platform = order.platform,
                type = "type1"
            )
        } returns Unit
        coEvery {
            orderValidationMetrics.onOrderValidationSuccess(
                platform = order.platform,
                type = "type2"
            )
        } returns Unit

        compositeOrderValidator.validate(order)
    }

    @Test
    fun `validator1 not suported`() = runBlocking<Unit> {
        coEvery { validator2.type } returns "type2"

        coEvery { validator2.validate(order) } returns Unit

        coEvery { validator1.supportsValidation(order) } returns false
        coEvery { validator2.supportsValidation(order) } returns true

        coEvery {
            orderValidationMetrics.onOrderValidationSuccess(
                platform = order.platform,
                type = "type2"
            )
        } returns Unit

        compositeOrderValidator.validate(order)
    }

    @Test
    fun `validator1 failed success`() = runBlocking<Unit> {
        coEvery { validator1.type } returns "type1"
        coEvery { validator2.type } returns "type2"

        coEvery { validator1.validate(order) } throws RuntimeException("test")
        coEvery { validator2.validate(order) } returns Unit

        coEvery { validator1.supportsValidation(order) } returns true
        coEvery { validator2.supportsValidation(order) } returns true

        coEvery {
            orderValidationMetrics.onOrderValidationFail(
                platform = order.platform,
                type = "type1"
            )
        } returns Unit
        coEvery {
            orderValidationMetrics.onOrderValidationSuccess(
                platform = order.platform,
                type = "type2"
            )
        } returns Unit

        assertThatExceptionOfType(RuntimeException::class.java).isThrownBy {
            runBlocking {
                compositeOrderValidator.validate(order)
            }
        }.withMessage("test")
    }
}
