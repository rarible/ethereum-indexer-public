package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class StatusOrderStateValidatorTest {
    @InjectMockKs
    private lateinit var statusOrderStateValidator: StatusOrderStateValidator

    @MockK
    private lateinit var orderUpdateService: OrderUpdateService

    @Test
    fun `validate and get not active`() = runBlocking<Unit> {
        val order = randomOrder(
            end = Instant.now().minusSeconds(100).epochSecond
        )
        coEvery { orderUpdateService.update(order.hash, any()) } returns createSellOrder()

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                statusOrderStateValidator.validate(order)
            }
        }.withMessage("order ${order.platform}:${order.hash} is not active")

        coVerify { orderUpdateService.update(order.hash, any()) }
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = randomOrder()

        statusOrderStateValidator.validate(order)
    }
}
