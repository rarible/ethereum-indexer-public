package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.data.createOrder
import com.rarible.protocol.order.api.service.order.validation.OrderStateValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class CompositeOrderStateValidatorTest {
    private lateinit var compositeOrderStateValidator: CompositeOrderStateValidator

    @MockK
    private lateinit var validator1: OrderStateValidator

    @MockK
    private lateinit var validator2: OrderStateValidator

    @BeforeEach
    fun before() {
        compositeOrderStateValidator = CompositeOrderStateValidator(listOf(validator1, validator2))
    }

    @Test
    fun validate() = runBlocking<Unit> {
        val order = createOrder()
        coEvery { validator1.validate(order) } returns Unit
        coEvery { validator2.validate(order) } returns Unit

        compositeOrderStateValidator.validate(order)

        coVerify {
            validator1.validate(order)
            validator2.validate(order)
        }
    }
}