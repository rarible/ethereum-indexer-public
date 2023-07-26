package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTInPairEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapInNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val sudoSwapInNftEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: PoolInfoProvider,
    private val sudoSwapCurve: PoolCurve,
    private val priceUpdateService: PriceUpdateService,
    private val sudoSwapLoad: SudoSwapLoadProperties,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) : PoolSubscriber<PoolTargetNftIn>(
    name = "sudo_nft_in_pair",
    topic = SwapNFTInPairEvent.id(),
    contracts = emptyList()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolTargetNftIn> {
        // TODO: Remove this in release 1.41
        if (log.address() in sudoSwapLoad.ignorePairs) {
            return emptyList()
        }
        val details = sudoSwapEventConverter.getSwapInNftDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val poolInfo = sudoSwapPoolInfoProvider.getPollInfo(hash, log.address()) ?: run {
            if (featureFlags.getPoolInfoFromChain) throw IllegalStateException("Can't get pool ${log.address()} info")
            else return emptyList()
        }
        val outputValue = sudoSwapCurve.getSellOutputValues(
            curve = poolInfo.curve,
            spotPrice = poolInfo.spotPrice,
            delta = poolInfo.delta,
            numItems = details.tokenIds.size,
            feeMultiplier = poolInfo.fee,
            protocolFeeMultiplier = poolInfo.protocolFee,
        )
        return details.tokenIds.mapIndexed { i, tokenId ->
            val amount = EthUInt256.of(outputValue[i].value)

            PoolTargetNftIn(
                hash = sudoSwapEventConverter.getPoolHash(log.address()),
                collection = poolInfo.collection,
                tokenIds = listOf(EthUInt256.of(tokenId)),
                tokenRecipient = details.tokenRecipient,
                inputValue = amount,
                date = timestamp,
                source = HistorySource.SUDOSWAP,
                priceUsd = priceUpdateService.getAssetUsdValue(poolInfo.currencyAssetType, amount.value, timestamp)
            )
        }.also { sudoSwapInNftEventCounter.increment() }
    }
}
