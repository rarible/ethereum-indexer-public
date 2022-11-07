package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.protocol.erc20.core.createRandomDepositEvent
import com.rarible.protocol.erc20.core.createRandomIncomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.createRandomTokenApprovalEvent
import com.rarible.protocol.erc20.core.createRandomWithdrawalEvent
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

internal class Erc20BalanceReducerTest {

    private val erc20BalanceMetricReducer = mockk<Erc20BalanceMetricReducer>()
    private val eventStatusErc20BalanceReducer = mockk<EventStatusErc20BalanceReducer>()
    private val erc20BalaReducer = Erc20BalanceReducer(
        erc20BalanceMetricReducer,
        eventStatusErc20BalanceReducer
    )

    @TestFactory
    fun `should reduce blockchain events`() = listOf(
        createRandomIncomeTransferEvent(),
        createRandomOutcomeTransferEvent(),
        createRandomWithdrawalEvent(),
        createRandomDepositEvent(),
        createRandomTokenApprovalEvent()
    ).map { event ->
        dynamicTest("should reduce blockchain events $event") {
            runBlocking {
                val entity = randomBalance()

                coEvery { erc20BalanceMetricReducer.reduce(entity, event) } returns entity
                coEvery { eventStatusErc20BalanceReducer.reduce(entity, event) } returns entity
                erc20BalaReducer.reduce(entity, event)

                coVerify(exactly = 1) { erc20BalanceMetricReducer.reduce(entity, event) }
                coVerify(exactly = 1) { eventStatusErc20BalanceReducer.reduce(entity, event) }
            }
        }
    }
}