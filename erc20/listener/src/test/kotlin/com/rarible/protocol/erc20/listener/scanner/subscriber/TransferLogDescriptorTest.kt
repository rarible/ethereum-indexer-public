package com.rarible.protocol.erc20.listener.scanner.subscriber

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.contract.model.Contract
import com.rarible.core.contract.model.ContractType
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import com.rarible.protocol.erc20.listener.service.Erc20RegistrationService
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import com.rarible.protocol.erc20.listener.test.log
import com.rarible.protocol.erc20.listener.test.transaction
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory
import java.time.Instant

internal class TransferLogDescriptorTest {

    private val contract: Contract = mockk() {
        every { type } returns ContractType.ERC20_TOKEN
        every { id } returns AddressFactory.create()
    }
    private val registrationService: Erc20RegistrationService = mockk() {
        coEvery { tryRegister(any()) } returns contract
    }
    private val ignoredOwnersResolver: IgnoredOwnersResolver = mockk() {
        every { resolve() } returns emptySet()
    }
    private val metrics: DescriptorMetrics = mockk() {
        every { onSaved() } returns Unit
        every { onSkipped(any()) } returns Unit
    }
    private val block: EthereumBlockchainBlock = mockk() {
        every { timestamp } returns Instant.now().epochSecond
    }

    @Test
    fun `shouldn't ignore event records`() = runBlocking<Unit> {
        val commonProps: Erc20ListenerProperties = mockk() {
            every { tokens } returns emptyList()
        }
        val descriptor = TransferLogSubscriber(
            registrationService,
            ignoredOwnersResolver,
            metrics,
            commonProps
        )
        val log: EthereumBlockchainLog = mockk() {
            every { ethLog } returns log(
                topics = listOf(
                    Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                    Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                    Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                ),
                data = "0x01",
            )
            every { index } returns 0
            every { ethTransaction } returns transaction()
        }

        val records = descriptor.getEthereumEventRecords(block, log)
        assertThat(records.size).isEqualTo(2)
        verify(exactly = 2) { metrics.onSaved() }
    }

    @Test
    fun `should ignore event record`() = runBlocking<Unit> {
        val commonProps: Erc20ListenerProperties = mockk() {
            every { tokens } returns listOf("0x3474606e53eae51f6a4f787e8c8d33999c6eae61")
        }
        val descriptor = TransferLogSubscriber(
            registrationService,
            ignoredOwnersResolver,
            metrics,
            commonProps
        )
        val log: EthereumBlockchainLog = mockk() {
            every { ethLog } returns log(
                topics = listOf(
                    Word.apply("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"),
                    Word.apply("0x0000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61"),
                    Word.apply("0x0000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c8935"),
                ),
                data = "0x01",
            )
            every { index } returns 0
            every { ethTransaction } returns transaction()
        }

        val records = descriptor.getEthereumEventRecords(block, log)
        assertThat(records.size).isEqualTo(0)
        verify(exactly = 2) { metrics.onSkipped(any()) }
    }
}
