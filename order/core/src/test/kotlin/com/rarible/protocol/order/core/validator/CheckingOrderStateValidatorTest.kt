package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.data.createOrderX2Y2DataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.OrderCancelService
import com.rarible.protocol.order.core.service.OrderStateCheckService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class CheckingOrderStateValidatorTest {

    private lateinit var checkingOrderStateValidator: CheckingOrderStateValidator

    @MockK
    private lateinit var orderStateCheckService: OrderStateCheckService

    @MockK
    private lateinit var orderCancelService: OrderCancelService

    @BeforeEach
    fun before() {
        checkingOrderStateValidator = CheckingOrderStateValidator(
            orderStateCheckService = orderStateCheckService,
            orderCancelService = orderCancelService,
            platform = Platform.X2Y2,
        )
    }

    @Test
    fun `validate x2y2`() = runBlocking<Unit> {
        val order = randomOrder().copy(
            platform = Platform.X2Y2,
            data = createOrderX2Y2DataV1()
        )
        coEvery { orderStateCheckService.isActiveOrder(order) } returns false
        coEvery {
            orderCancelService.cancelOrder(
                id = eq(order.hash),
                eventTimeMarksDto = any()
            )
        } returns createSellOrder()

        assertThatExceptionOfType(OrderDataException::class.java).isThrownBy {
            runBlocking {
                checkingOrderStateValidator.validate(order)
            }
        }.withMessage("order X2Y2:${order.hash} is not active")

        coVerify {
            orderCancelService.cancelOrder(id = eq(order.hash), eventTimeMarksDto = any())
        }
    }

    @Test
    fun `validate x2y2 exception`() = runBlocking<Unit> {
        val order = randomOrder().copy(
            platform = Platform.X2Y2,
            data = createOrderX2Y2DataV1()
        )
        coEvery { orderStateCheckService.isActiveOrder(order) } throws IllegalStateException("error")

        checkingOrderStateValidator.validate(order)
    }

    @Test
    fun `supports - false`() = runBlocking<Unit> {
        val order = randomOrder()

        assertThat(checkingOrderStateValidator.supportsValidation(order)).isFalse()
    }

    @Test
    fun `validate x2y2 valid`() = runBlocking<Unit> {
        val order = randomOrder().copy(
            platform = Platform.X2Y2,
            data = createOrderX2Y2DataV1()
        )
        coEvery { orderStateCheckService.isActiveOrder(order) } returns true

        checkingOrderStateValidator.validate(order)
    }
}
