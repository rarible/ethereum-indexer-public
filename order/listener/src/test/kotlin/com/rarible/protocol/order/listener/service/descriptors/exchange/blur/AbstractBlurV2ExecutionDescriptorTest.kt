package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.blur.BlurV2EventConverter
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import scalether.domain.Address
import scalether.domain.response.Transaction
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Instant

abstract class AbstractBlurV2ExecutionDescriptorTest {
    protected val sender = mockk<MonoTransactionSender>()
    protected val blockTimestamp: Instant = Instant.ofEpochSecond(Instant.now().epochSecond)
    protected val weth = randomAddress()
    protected val market = randomAddress()
    protected val contractsProvider = mockk<ContractsProvider> {
        every { blurV2() } returns listOf(market)
        every { weth() } returns weth
    }
    protected val traceCallService = mockk<TraceCallService>()
    protected val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    protected val mockkBlock = mockk<EthereumBlockchainBlock> {
        every { timestamp } returns blockTimestamp.epochSecond
    }
    private val contractService = mockk<ContractService> {
        coEvery { get(weth) } returns Erc20Token(weth, "weth", "w", 18)
    }
    private val prizeNormalizer = PriceNormalizer(contractService)
    protected val priceUpdateService = mockk<PriceUpdateService>()

    protected val blurV2EventConverter = BlurV2EventConverter(
        traceCallService = traceCallService,
        featureFlags = featureFlags,
        contractsProvider = contractsProvider,
        prizeNormalizer = prizeNormalizer,
        priceUpdateService = priceUpdateService,
        sender = sender
    )

    protected fun mockkTransaction(
        input: Binary,
        from: Address = randomAddress()
    ) = mockk<Transaction> {
        every { input() } returns input
        every { hash() } returns Word.apply(randomWord())
        every { from() } returns from
        every { to() } returns randomAddress()
    }

    protected suspend inline fun <reified T : OrderExchangeHistory> ExchangeSubscriber<T>.convert(
        block: EthereumBlockchainBlock,
        log: EthereumBlockchainLog
    ): List<T> {
        return getEthereumEventRecords(block, log)
            .filterIsInstance<ReversedEthereumLogRecord>()
            .map { it.data as T }
    }

    protected fun mockkServerToGetNonce(
        trader: Address,
        market: Address,
        nonoce: BigInteger
    ) {
    }
}
