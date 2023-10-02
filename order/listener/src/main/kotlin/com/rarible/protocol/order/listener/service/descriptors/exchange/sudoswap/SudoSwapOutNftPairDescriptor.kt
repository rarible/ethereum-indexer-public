package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.model.SudoSwapAnyOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapTargetOutNftDetail
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.listener.configuration.SudoSwapLoadProperties
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.PoolSubscriber
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapNftTransferDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@EnableSudoSwap
class SudoSwapOutNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val nftTransferDetector: SudoSwapNftTransferDetector,
    private val sudoSwapOutNftEventCounter: RegisteredCounter,
    private val wrapperSudoSwapMatchEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: PoolInfoProvider,
    private val poolCurve: PoolCurve,
    private val priceUpdateService: PriceUpdateService,
    private val sudoSwapLoad: SudoSwapLoadProperties,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    autoReduceService: AutoReduceService,
) : PoolSubscriber<PoolTargetNftOut>(
    name = "sudo_nft_out_pair",
    topic = SwapNFTOutPairEvent.id(),
    contracts = emptyList(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<PoolTargetNftOut> {
        logger.info("log=$log, transaction=$transaction, index=$index, totalLogs=$totalLogs")
        if (log.address() in sudoSwapLoad.ignorePairs) {
            return emptyList()
        }
        val details = sudoSwapEventConverter.getSwapOutNftDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val poolInfo = sudoSwapPoolInfoProvider.getPollInfo(hash, log.address()) ?: run {
            if (featureFlags.getPoolInfoFromChain) throw IllegalStateException("Can't get pool ${log.address()} info")
            else return emptyList()
        }
        val tokenIds = when (details) {
            is SudoSwapAnyOutNftDetail -> {
                logger.info("Detected swapTokenForAnyNFTs method call in tx=${transaction.hash()}")
                val tokenIds = nftTransferDetector.detectNftTransfers(
                    sudoSwapNftOutPairLog = log,
                    nftCollection = poolInfo.collection
                )
                require(tokenIds.size == details.numberNft.toInt()) {
                    "Found tokenIds amount (${tokenIds.size}) didn't much event nft out number (${details.numberNft.toInt()}), tx=${transaction.hash()}, logIndex=${log.logIndex()}"
                }
                tokenIds
            }
            is SudoSwapTargetOutNftDetail -> {
                details.nft
            }
        }
        val outputValue = poolCurve.getBuyInputValues(
            curve = poolInfo.curve,
            spotPrice = poolInfo.spotPrice,
            delta = poolInfo.delta,
            numItems = tokenIds.size,
            feeMultiplier = poolInfo.fee,
            protocolFeeMultiplier = poolInfo.protocolFee,
        )
        return tokenIds.mapIndexed { i, tokenId ->
            val amount = EthUInt256.of(outputValue[i].value)

            PoolTargetNftOut(
                hash = hash,
                collection = poolInfo.collection,
                tokenIds = listOf(EthUInt256.of(tokenId)),
                recipient = details.nftRecipient,
                outputValue = amount,
                date = timestamp,
                source = HistorySource.SUDOSWAP,
                priceUsd = priceUpdateService.getAssetUsdValue(poolInfo.currencyAssetType, amount.value, timestamp)
            ).let {
                PoolTargetNftOut.addMarketplaceMarker(it, transaction.input(), wrapperSudoSwapMatchEventCounter)
            }
        }.also { sudoSwapOutNftEventCounter.increment() }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SudoSwapOutNftPairDescriptor::class.java)
    }
}
