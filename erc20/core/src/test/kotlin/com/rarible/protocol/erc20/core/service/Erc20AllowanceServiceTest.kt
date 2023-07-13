package com.rarible.protocol.erc20.core.service

import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.event.Erc20BalanceEventListener
import com.rarible.protocol.erc20.core.model.Erc20Allowance
import com.rarible.protocol.erc20.core.model.Erc20AllowanceEvent
import com.rarible.protocol.erc20.core.repository.Erc20AllowanceRepository
import com.rarible.protocol.erc20.core.repository.data.randomAllowance
import com.rarible.protocol.erc20.core.repository.data.randomBalanceId
import io.daonomic.rpc.domain.Binary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import scalether.abi.Int32Type
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoTransactionSender

@ExtendWith(MockKExtension::class)
internal class Erc20AllowanceServiceTest {
    private lateinit var erc20AllowanceService: Erc20AllowanceService

    @MockK
    private lateinit var sender: MonoTransactionSender

    @MockK
    private lateinit var erc20AllowanceRepository: Erc20AllowanceRepository

    private var erc20BalanceEventListeners: MutableList<Erc20BalanceEventListener> = mutableListOf()

    @MockK
    private lateinit var erc20BalanceEventListener: Erc20BalanceEventListener

    @SpyK
    private var erc20IndexerProperties: Erc20IndexerProperties = Erc20IndexerProperties(
        blockchain = Blockchain.ETHEREUM,
        erc20TransferProxy = Address.ONE(),
    )

    @BeforeEach
    fun before() {
        erc20BalanceEventListeners.add(erc20BalanceEventListener)
        erc20AllowanceService = Erc20AllowanceService(
            sender = sender,
            erc20AllowanceRepository = erc20AllowanceRepository,
            erc20BalanceEventListeners = erc20BalanceEventListeners,
            erc20IndexerProperties = erc20IndexerProperties,
        )
    }

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()
        val chainValue = EthUInt256.of(randomBigInt())
        val allowance = randomAllowance(token = balanceId.token, owner = balanceId.owner)

        coEvery { erc20AllowanceRepository.get(balanceId) } returns allowance

        coEvery {
            erc20AllowanceRepository.save(match {
                it.owner == balanceId.owner && it.token == balanceId.token && it.allowance == chainValue
            })
        } answers {
            it.invocation.args.first() as Erc20Allowance
        }
        every {
            sender.call(
                Transaction(
                    balanceId.token,
                    null,
                    null,
                    null,
                    null,
                    //Call ERC20 balance method
                    Binary.apply(
                        "0xdd62ed3e000000000000000000000000${balanceId.owner.hex()}" +
                            "000000000000000000000000${Address.ONE().hex()}"
                    ),
                    null
                )
            )
        } returns Mono.just(Int32Type.encode(chainValue.value))

        coEvery { erc20BalanceEventListener.onUpdate(any()) } returns Unit

        erc20AllowanceService.onChainUpdate(balanceId, null)

        coVerify {
            erc20BalanceEventListener.onUpdate(withArg {
                val allowanceEvent = (it as Erc20AllowanceEvent)
                assertThat(allowanceEvent.allowance.allowance).isEqualTo(chainValue)
                assertThat(allowanceEvent.allowance.id).isEqualTo(balanceId)
            })
        }
    }

    @Test
    fun `update - allowance not found in db`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()
        val chainValue = EthUInt256.of(randomBigInt())

        coEvery { erc20AllowanceRepository.get(balanceId) } returns null

        coEvery {
            erc20AllowanceRepository.save(match {
                it.owner == balanceId.owner && it.token == balanceId.token && it.allowance == chainValue
            })
        } answers {
            it.invocation.args.first() as Erc20Allowance
        }
        every {
            sender.call(
                Transaction(
                    balanceId.token,
                    null,
                    null,
                    null,
                    null,
                    //Call ERC20 balance method
                    Binary.apply(
                        "0xdd62ed3e000000000000000000000000${balanceId.owner.hex()}" +
                            "000000000000000000000000${Address.ONE().hex()}"
                    ),
                    null
                )
            )
        } returns Mono.just(Int32Type.encode(chainValue.value))

        coEvery { erc20BalanceEventListener.onUpdate(any()) } returns Unit

        erc20AllowanceService.onChainUpdate(balanceId, null)

        coVerify {
            erc20BalanceEventListener.onUpdate(withArg {
                val allowanceEvent = (it as Erc20AllowanceEvent)
                assertThat(allowanceEvent.allowance.allowance).isEqualTo(chainValue)
                assertThat(allowanceEvent.allowance.id).isEqualTo(balanceId)
            })
        }
    }

    @Test
    fun `update - allowance not found in blockchain`() = runBlocking<Unit> {
        val balanceId = randomBalanceId()
        val chainValue = EthUInt256.of(randomBigInt())

        coEvery { erc20AllowanceRepository.get(balanceId) } returns null

        every {
            sender.call(
                Transaction(
                    balanceId.token,
                    null,
                    null,
                    null,
                    null,
                    //Call ERC20 balance method
                    Binary.apply(
                        "0xdd62ed3e000000000000000000000000${balanceId.owner.hex()}" +
                            "000000000000000000000000${Address.ONE().hex()}"
                    ),
                    null
                )
            )
        } returns Mono.empty()


        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            runBlocking {
                erc20AllowanceService.onChainUpdate(balanceId, null)
            }
        }
    }
}