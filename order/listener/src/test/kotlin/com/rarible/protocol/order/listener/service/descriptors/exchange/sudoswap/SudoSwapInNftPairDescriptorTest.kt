package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomPoolInfo
import com.rarible.protocol.order.core.data.randomSudoSwapPurchaseValue
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.data.log
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
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class SudoSwapInNftPairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val sudoSwapPoolInfoProvider = mockk<PoolInfoProvider>()
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapCurve = mockk<PoolCurve>()
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val sudoSwapLoad = mockk<SudoSwapLoadProperties>()
    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { getAssetUsdValue(any(), any(), any()) } returns BigDecimal.ONE
    }

    private val descriptor = SudoSwapInNftPairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        sudoSwapInNftEventCounter = counter,
        sudoSwapPoolInfoProvider = sudoSwapPoolInfoProvider,
        sudoSwapCurve = sudoSwapCurve,
        priceUpdateService = priceUpdateService,
        featureFlags = OrderIndexerProperties.FeatureFlags(),
        sudoSwapLoad = sudoSwapLoad
    )

    @Test
    fun `should convert target nft in pair`() = runBlocking<Unit> {
        // from:
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0xb1d3f1c100000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000046e9ab8cad0c0c00000000000000000000000003cb23ccc26a1870eb9e79b7a061907bdaef4f7d60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000001841")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val poolInfo = randomPoolInfo()
        val purchaseValue = randomSudoSwapPurchaseValue()
        val log = log(
            listOf(
                Word.apply("0x3614eb567740a0ee3897c0e2b11ad6a5720d2e4438f9c8accf6c95c24af3a470")
            ),
            ""
        )
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())

        every { sudoSwapLoad.ignorePairs } returns emptySet()
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo
        coEvery { sudoSwapCurve.getSellOutputValues(poolInfo.curve, poolInfo.spotPrice, poolInfo.delta, 1, poolInfo.fee, poolInfo.protocolFee) } returns listOf(purchaseValue)

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
        val nftOut = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data }
            .single()

        assertThat(nftOut).isInstanceOf(PoolTargetNftIn::class.java)
        nftOut as PoolTargetNftIn

        assertThat(nftOut.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        assertThat(nftOut.collection).isEqualTo(poolInfo.collection)
        assertThat(nftOut.tokenIds).containsExactlyInAnyOrder(EthUInt256.of(6209))
        assertThat(nftOut.tokenRecipient).isEqualTo(Address.apply("0x3Cb23ccc26a1870eb9E79B7A061907BDaeF4F7D6"))
        assertThat(nftOut.date).isEqualTo(date)
        assertThat(nftOut.inputValue.value).isEqualTo(purchaseValue.value)
        assertThat(nftOut.source).isEqualTo(HistorySource.SUDOSWAP)
        assertThat(nftOut.priceUsd).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `convert - ignore pair`() = runBlocking<Unit> {
        val transaction = mockk<Transaction>()
        val log = log()
        every { sudoSwapLoad.ignorePairs } returns setOf(log.address())

        val ethBlock = EthereumBlockchainBlock(
            number = 1,
            hash = randomWord(),
            parentHash = randomWord(),
            timestamp = Instant.now().epochSecond,
            ethBlock = mockk()
        )
        val ethLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transaction,
            index = 0,
            total = 1,
        )
        val nftOut = descriptor
            .getEthereumEventRecords(ethBlock, ethLog)
            .map { it.asEthereumLogRecord().data }
            .firstOrNull()

        assertThat(nftOut).isNull()
    }
}
