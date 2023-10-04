package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.randomPoolInfo
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolFeeUpdate
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
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

internal class SudoSwapFeeUpdatePairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val autoReduceService = mockk<AutoReduceService>()
    private val sudoSwapPoolInfoProvider = mockk<PoolInfoProvider>()

    private val descriptor = SudoSwapFeeUpdatePairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapUpdateFeeEventCounter = counter,
        autoReduceService = autoReduceService,
        sudoSwapPoolInfoProvider = sudoSwapPoolInfoProvider,
        sudoSwapLoad = SudoSwapLoadProperties(ignoreCollections = setOf(Address.ONE())),
    )

    @Test
    fun `should convert update fee in pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x2fbde7eb2bb0ec4a84377b4154980bb5c22a80bf1ddcd668db0e795eff5ec7b2
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x66c55c30868c51e7ad52e3d85d1403576a9967614e67c48e25b55a10baa650c0")
            ),
            "00000000000000000000000000000000000000000000000002c68af0bb140000"
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
        val poolInfo = randomPoolInfo()
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo
        val update = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data as PoolFeeUpdate }
            .single()

        assertThat(update.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        assertThat(update.newFee).isEqualTo(BigInteger("200000000000000000"))
        assertThat(update.date).isEqualTo(date)
        assertThat(update.source).isEqualTo(HistorySource.SUDOSWAP)
    }

    @Test
    fun `ignore collection`() = runBlocking<Unit> {
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val log = log(
            listOf(
                Word.apply("0x66c55c30868c51e7ad52e3d85d1403576a9967614e67c48e25b55a10baa650c0")
            ),
            "00000000000000000000000000000000000000000000000002c68af0bb140000"
        )
        val poolInfo = randomPoolInfo().copy(collection = Address.ONE())
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo
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
        val update = descriptor.getEthereumEventRecords(ethBlock, ethLog)

        assertThat(update).isEmpty()
    }
}
