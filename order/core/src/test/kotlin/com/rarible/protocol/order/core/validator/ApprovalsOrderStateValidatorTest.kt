package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ApprovalsOrderStateValidatorTest {
    @InjectMockKs
    private lateinit var approvalsOrderStateValidator: ApprovalsOrderStateValidator

    @MockK
    private lateinit var approveService: ApproveService

    @MockK
    private lateinit var orderUpdateService: OrderUpdateService

    @Test
    fun `validate and get no approval`() = runBlocking<Unit> {
        val order = randomOrder()
        coEvery {
            orderUpdateService.updateApproval(
                order = order,
                approved = false,
                eventTimeMarks = any()
            )
        } returns Unit
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns false
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns true

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                approvalsOrderStateValidator.validate(order)
            }
        }.withMessage("order ${order.platform}:${order.hash} is not approved")

        coVerify { orderUpdateService.updateApproval(order = order, approved = false, eventTimeMarks = any()) }
    }

    @Test
    fun `validate and get no erc20 allowance`() = runBlocking<Unit> {
        val order = randomOrder()
        coEvery {
            orderUpdateService.updateApproval(
                order = order,
                approved = false,
                eventTimeMarks = any()
            )
        } returns Unit
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns true
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns false

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                approvalsOrderStateValidator.validate(order)
            }
        }.withMessage("order ${order.platform}:${order.hash} is not approved")

        coVerify { orderUpdateService.updateApproval(order = order, approved = false, eventTimeMarks = any()) }
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = randomOrder()

        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns true
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns true

        approvalsOrderStateValidator.validate(order)
    }
}
