package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.randomPoolInfo
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolSpotPriceUpdate
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

internal class SudoSwapSpotPriceUpdatePairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val autoReduceService = mockk<AutoReduceService>()
    private val sudoSwapPoolInfoProvider = mockk<PoolInfoProvider>()

    private val descriptor = SudoSwapSpotPriceUpdatePairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapUpdateSpotPriceEventCounter = counter,
        autoReduceService = autoReduceService,
        sudoSwapPoolInfoProvider = sudoSwapPoolInfoProvider,
        sudoSwapLoad = SudoSwapLoadProperties(ignoreCollections = setOf(Address.ONE())),
    )

    @Test
    fun `should convert update spot price to pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0xacd9c875bf76a7181af3c379b547d59e93efa3dfbe781127b0c66d9b170f15e6#eventlog
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
                Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")
            ),
            "0x000000000000000000000000000000000000000000000000048ed26aaef2fa50"
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
            .map { it.asEthereumLogRecord().data as PoolSpotPriceUpdate }
            .single()

        assertThat(update.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        assertThat(update.newSpotPrice).isEqualTo(BigInteger("328431178488150608"))
        assertThat(update.date).isEqualTo(date)
        assertThat(update.source).isEqualTo(HistorySource.SUDOSWAP)
    }

    @Test
    fun `should ignore collection`() = runBlocking<Unit> {
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
                Word.apply("0xf06180fdbe95e5193df4dcd1352726b1f04cb58599ce58552cc952447af2ffbb")
            ),
            "0x000000000000000000000000000000000000000000000000048ed26aaef2fa50"
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
        val poolInfo = randomPoolInfo().copy(collection = Address.ONE())
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo

        val update = descriptor.getEthereumEventRecords(ethBlock, ethLog)

        assertThat(update).isEmpty()
    }
}
