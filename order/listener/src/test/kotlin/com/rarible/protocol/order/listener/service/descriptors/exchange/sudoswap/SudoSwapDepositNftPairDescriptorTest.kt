package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapDepositNftPairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val contractsProvider = mockk<ContractsProvider> {
        every { pairFactoryV1() } returns listOf(randomAddress())
    }
    private val autoReduceService = mockk<AutoReduceService>()
    private val descriptor = SudoSwapDepositNftPairDescriptor(
        contractsProvider = contractsProvider,
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapDepositNftEventCounter = counter,
        autoReduceService = autoReduceService,
        sudoSwapLoad = SudoSwapLoadProperties(ignoreCollections = setOf(Address.ONE())),
    )

    @Test
    fun `should convert nft deposit to pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x75bd9bf3d680c54c67055da9fd84316b6249c5284224d3e59e2618237f621b14
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0x2cdb394b0000000000000000000000001895c2da9155d7720a7957da06ce898a6a29d0a70000000000000000000000000000000000000000000000000000000000000060000000000000000000000000b4d6af08afb69fe9d190731ab4fbaf9f899ee46f000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000006d80000000000000000000000000000000000000000000000000000000000000ba50000000000000000000000000000000000000000000000000000000000000ba3")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x4fd0cd7c14badac45ff0bee670a9d8dd80e87907afcf2c121e0fd4b8b4b0047f")
            ),
            "0x000000000000000000000000b4d6af08afb69fe9d190731ab4fbaf9f899ee46f"
        )
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date.epochSecond,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = 0,
            total = 1,
        )
        val deposit = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data as PoolNftDeposit }
            .single()

        assertThat(deposit.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(Address.apply("0xB4d6af08Afb69FE9D190731aB4FbAF9F899Ee46f")))
        assertThat(deposit.collection).isEqualTo(Address.apply("0x1895C2da9155d7720a7957dA06Ce898A6a29d0A7"))
        assertThat(deposit.tokenIds).containsExactlyInAnyOrder(
            EthUInt256.of(1752),
            EthUInt256.of(2981),
            EthUInt256.of(2979),
        )
        assertThat(deposit.date).isEqualTo(date)
        assertThat(deposit.source).isEqualTo(HistorySource.SUDOSWAP)
    }

    @Test
    fun `ignore collection`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0x2cdb394b00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000060000000000000000000000000b4d6af08afb69fe9d190731ab4fbaf9f899ee46f000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000006d80000000000000000000000000000000000000000000000000000000000000ba50000000000000000000000000000000000000000000000000000000000000ba3")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x4fd0cd7c14badac45ff0bee670a9d8dd80e87907afcf2c121e0fd4b8b4b0047f")
            ),
            "0x000000000000000000000000b4d6af08afb69fe9d190731ab4fbaf9f899ee46f"
        )
        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = date.epochSecond,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = 0,
            total = 1,
        )
        val result = descriptor.getEthereumEventRecords(ethBlock, ethLog)

        assertThat(result).isEmpty()
    }
}
