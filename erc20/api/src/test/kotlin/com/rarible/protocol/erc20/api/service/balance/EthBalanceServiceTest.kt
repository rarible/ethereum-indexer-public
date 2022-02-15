package com.rarible.protocol.erc20.api.service.balance

import com.rarible.core.test.data.randomAddress
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import java.math.BigDecimal
import java.math.BigInteger

internal class EthBalanceServiceTest {

    private val ethereum: MonoEthereum = mockk()

    private val ethBalanceService = EthBalanceService(ethereum)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ethereum)
    }

    @Test
    fun `get balance`() = runBlocking<Unit> {
        val address = randomAddress()
        val expectedBalance = BigInteger.valueOf(2_000_000_000_000_000_000) // 2e18

        coEvery { ethereum.ethGetBalance(address, "latest") } returns expectedBalance.toMono()

        val balance = ethBalanceService.getBalance(address)

        assertThat(balance.owner).isEqualTo(address)
        assertThat(balance.balance).isEqualTo(expectedBalance)
        assertThat(balance.decimalBalance.stripTrailingZeros()).isEqualTo(BigDecimal.valueOf(2))
    }
}