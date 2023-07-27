package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomPoolInfo
import com.rarible.protocol.order.core.data.randomSudoSwapPurchaseValue
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapNftTransferDetector
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

internal class SudoSwapOutNftPairDescriptorTest {
    private val counter = mockk<RegisteredCounter> { every { increment() } returns Unit }
    private val traceCallService = TraceCallServiceImpl(mockk(), mockk())
    private val sudoSwapEventConverter = SudoSwapEventConverter(traceCallService)
    private val nftTransferDetector = mockk<SudoSwapNftTransferDetector>()
    private val sudoSwapPoolInfoProvider = mockk<PoolInfoProvider>()
    private val sudoSwapCurve = mockk<PoolCurve>()
    private val sudoSwapLoad = mockk<SudoSwapLoadProperties>()
    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { getAssetUsdValue(any(), any(), any()) } returns BigDecimal.ONE
    }

    private val descriptor = SudoSwapOutNftPairDescriptor(
        sudoSwapEventConverter = sudoSwapEventConverter,
        nftTransferDetector = nftTransferDetector,
        sudoSwapPoolInfoProvider = sudoSwapPoolInfoProvider,
        sudoSwapOutNftEventCounter = counter,
        wrapperSudoSwapMatchEventCounter = counter,
        poolCurve = sudoSwapCurve,
        priceUpdateService = priceUpdateService,
        featureFlags = OrderIndexerProperties.FeatureFlags(),
        sudoSwapLoad = sudoSwapLoad
    )

    @Test
    fun `should convert target nft out pair`() = runBlocking<Unit> {
        // from: https://etherscan.io/tx/0x31801c73e46e6b526ece72a16738f54ed4d501bc735737b261eea0377ac85b76
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0x6d8b99f700000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000002bc120d8ac42604000000000000000000000000c2681d0606ebd7719040f2bc1c0fda3e9215db900000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c2681d0606ebd7719040f2bc1c0fda3e9215db900000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000351c00000000000000000000000000000000000000000000000109616c6c64617461")
            every { value() } returns BigInteger.ZERO
        }
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val poolInfo = randomPoolInfo()
        val purchaseValue = randomSudoSwapPurchaseValue()
        val log = log(
            listOf(
                Word.apply("0xbc479dfc6cb9c1a9d880f987ee4b30fa43dd7f06aec121db685b67d587c93c93")
            ),
            ""
        )
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())

        every { sudoSwapLoad.ignorePairs } returns emptySet()
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo
        coEvery {
            sudoSwapCurve.getBuyInputValues(
                poolInfo.curve,
                poolInfo.spotPrice,
                poolInfo.delta,
                1,
                poolInfo.fee,
                poolInfo.protocolFee
            )
        } returns listOf(purchaseValue)

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
            .firstOrNull()

        assertThat(nftOut).isInstanceOf(PoolTargetNftOut::class.java)
        nftOut as PoolTargetNftOut

        assertThat(nftOut.hash).isEqualTo(sudoSwapEventConverter.getPoolHash(log.address()))
        assertThat(nftOut.collection).isEqualTo(poolInfo.collection)
        assertThat(nftOut.tokenIds).containsExactlyInAnyOrder(EthUInt256.of(13596))
        assertThat(nftOut.recipient).isEqualTo(Address.apply("0xc2681D0606EbD7719040f2Bc1c0fdA3E9215Db90"))
        assertThat(nftOut.date).isEqualTo(date)
        assertThat(nftOut.outputValue.value).isEqualTo(purchaseValue.value)
        assertThat(nftOut.source).isEqualTo(HistorySource.SUDOSWAP)
        assertThat(nftOut.marketplaceMarker).isNotNull
    }

    @Test
    fun `should convert any nft out pair`() = runBlocking<Unit> {
        // from: https://rinkeby.etherscan.io/tx/0xb5acee7a63e2ad319a1532a5d0bd19dc268579269be92c7fdbb94fac9ab78b54
        val transaction = mockk<Transaction> {
            every { hash() } returns Word.apply(randomWord())
            every { from() } returns randomAddress()
            every { to() } returns randomAddress()
            every { input() } returns Binary.apply("0x28b8aee100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000023e1e5803b40000000000000000000000000007f4b31f1578bb1d49d420379b7db20d08a5c893500000000000000000000000000000000000000000000000000000000000000000000000000000000000000003474606e53eae51f6a4f787e8c8d33999c6eae61")
            every { value() } returns BigInteger.ZERO
        }
        val log = log(
            listOf(
                Word.apply("0xbc479dfc6cb9c1a9d880f987ee4b30fa43dd7f06aec121db685b67d587c93c93")
            ),
            ""
        )
        val orderHash = sudoSwapEventConverter.getPoolHash(log.address())
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val poolInfo = randomPoolInfo()
        val purchaseValue = randomSudoSwapPurchaseValue()
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val expectedTokenId = randomBigInt()
        every { sudoSwapLoad.ignorePairs } returns emptySet()
        coEvery { sudoSwapPoolInfoProvider.getPollInfo(orderHash, log.address()) } returns poolInfo
        coEvery { nftTransferDetector.detectNftTransfers(log, poolInfo.collection) } returns listOf(expectedTokenId)
        coEvery {
            sudoSwapCurve.getBuyInputValues(
                poolInfo.curve,
                poolInfo.spotPrice,
                poolInfo.delta,
                1,
                poolInfo.fee,
                poolInfo.protocolFee
            )
        } returns listOf(purchaseValue)

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
            .firstOrNull()

        assertThat(nftOut).isInstanceOf(PoolTargetNftOut::class.java)
        nftOut as PoolTargetNftOut
        assertThat(nftOut.hash).isEqualTo(hash)
        assertThat(nftOut.tokenIds).containsExactlyInAnyOrder(EthUInt256.of(expectedTokenId))
        assertThat(nftOut.recipient).isEqualTo(Address.apply("0x7F4B31F1578bB1d49D420379b7Db20d08A5c8935"))
        assertThat(nftOut.outputValue.value).isEqualTo(purchaseValue.value)
        assertThat(nftOut.date).isEqualTo(date)
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
