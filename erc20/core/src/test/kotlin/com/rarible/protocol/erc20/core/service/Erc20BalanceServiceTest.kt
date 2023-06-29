package com.rarible.protocol.erc20.core.service

import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.event.Erc20BalanceEventListener
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.core.repository.data.randomBalance
import com.rarible.protocol.erc20.core.repository.data.randomBalanceId
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.abi.Int32Type
import scalether.domain.request.Transaction
import scalether.transaction.MonoTransactionSender

class Erc20BalanceServiceTest {
    private val sender = mockk<MonoTransactionSender>()
    private val erc20BalanceRepository = mockk<Erc20BalanceRepository>()
    private val erc20BalanceEventListeners = mockk<Erc20BalanceEventListener>()
    private val service = Erc20BalanceService(sender, erc20BalanceRepository, listOf(erc20BalanceEventListeners))

    @Test
    @Suppress("ReactiveStreamsUnusedPublisher")
    fun `update on chain - ok`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()
        val chainValue = EthUInt256.of(randomBigInt())
        val balance = randomBalance(balanceId.token, balanceId.owner)

        coEvery { erc20BalanceRepository.get(balanceId) } returns balance

        coEvery { erc20BalanceRepository.save(balance.withBalance(chainValue)) } answers {
            it.invocation.args.first() as Erc20Balance
        }
        every { sender.call(
            Transaction(
                balanceId.token,
                null,
                null,
                null,
                null,
                //Call ERC20 balance method
                Binary.apply("0x70a08231000000000000000000000000${balanceId.owner.hex()}"),
                null
            )
        ) } returns Mono.just(Int32Type.encode(chainValue.value))

        coEvery { erc20BalanceEventListeners.onUpdate(any()) } returns Unit

        val updated = service.onChainUpdate(balanceId, null)
        assertThat(updated).isNotNull
        assertThat(updated?.balance).isEqualTo(chainValue)

        coVerify { erc20BalanceEventListeners.onUpdate(withArg {
            assertThat(it.balance).isEqualTo(updated)
        }) }
    }
}