package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.model.SudoSwapAnyOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapTargetOutNftDetail
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapNftTransferDetector
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableSudoSwap
class SudoSwapOutNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val nftTransferDetector: SudoSwapNftTransferDetector,
    private val sudoSwapOutNftEventCounter: RegisteredCounter,
    private val wrapperSudoSwapMatchEventCounter: RegisteredCounter,
    private val sudoSwapPoolInfoProvider: PoolInfoProvider,
    private val poolCurve: PoolCurve,
    private val priceUpdateService: PriceUpdateService
): LogEventDescriptor<PoolTargetNftOut> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = SwapNFTOutPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolTargetNftOut> {
        return mono { convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): List<PoolTargetNftOut> {
        val details = sudoSwapEventConverter.getSwapOutNftDetails(log.address(), transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        val poolInfo = sudoSwapPoolInfoProvider.getPollInfo(hash, log.address())
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
                date = date,
                source = HistorySource.SUDOSWAP,
                priceUsd = priceUpdateService.getAssetUsdValue(poolInfo.currencyAssetType, amount.value, date)
            ).let {
                PoolTargetNftOut.addMarketplaceMarker(it, transaction.input(), wrapperSudoSwapMatchEventCounter)
            }
        }.also { sudoSwapOutNftEventCounter.increment() }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SudoSwapOutNftPairDescriptor::class.java)
    }
}
