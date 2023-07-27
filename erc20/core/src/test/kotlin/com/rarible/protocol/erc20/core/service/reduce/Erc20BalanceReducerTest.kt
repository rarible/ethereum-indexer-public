package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.randomDepositEvent
import com.rarible.protocol.erc20.core.randomIncomeTransferEvent
import com.rarible.protocol.erc20.core.randomOutcomeTransferEvent
import com.rarible.protocol.erc20.core.randomTokenApprovalEvent
import com.rarible.protocol.erc20.core.randomWithdrawalEvent
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
        randomIncomeTransferEvent(),
        randomOutcomeTransferEvent(),
        randomWithdrawalEvent(),
        randomDepositEvent(),
        randomTokenApprovalEvent()
    ).map { event ->
        dynamicTest("should reduce blockchain events $event") {
            runBlocking {
                val entity = randomBalance()

                coEvery { erc20BalanceMetricReducer.reduce(entity, event) } returns entity
                coEvery { eventStatusErc20BalanceReducer.reduce(entity, event) } returns entity
                erc20BalaReducer.reduce(entity, Erc20MarkedEvent(event))

                coVerify(exactly = 1) { erc20BalanceMetricReducer.reduce(entity, event) }
                coVerify(exactly = 1) { eventStatusErc20BalanceReducer.reduce(entity, event) }
            }
        }
    }
}
