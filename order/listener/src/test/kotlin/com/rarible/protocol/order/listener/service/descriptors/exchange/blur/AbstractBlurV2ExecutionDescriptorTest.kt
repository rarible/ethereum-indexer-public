package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.blur.BlurV2EventConverter
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import scalether.domain.response.Transaction
import java.time.Instant

abstract class AbstractBlurV2ExecutionDescriptorTest {
    private val contractService = mockk<ContractService>()
    private val prizeNormalizer = PriceNormalizer(contractService)
    protected val blockTimestamp = Instant.ofEpochSecond(Instant.now().epochSecond)
    protected val contractsProvider = mockk<ContractsProvider> {
        every { blurV2() } returns listOf(randomAddress())
    }
    protected val traceCallService = mockk<TraceCallService>()
    protected val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    protected val mockkBlock = mockk<EthereumBlockchainBlock> {
        every { timestamp } returns blockTimestamp.epochSecond
    }

    protected val blurV2EventConverter = BlurV2EventConverter(
        traceCallService = traceCallService,
        featureFlags = featureFlags,
        prizeNormalizer = prizeNormalizer,
    )

    protected fun mockkTransaction(input: Binary) = mockk<Transaction> {
        every { input() } returns input
        every { hash() } returns Word.apply(randomWord())
        every { from() } returns randomAddress()
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
}