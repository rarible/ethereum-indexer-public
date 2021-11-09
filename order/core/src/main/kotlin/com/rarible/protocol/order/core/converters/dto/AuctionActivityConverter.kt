package com.rarible.protocol.order.core.converters.dto

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.time.Instant

@Component
class AuctionActivityConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter,
    private val auctionBidDtoConverter: AuctionBidDtoConverter,
    private val auctionRepository: AuctionRepository
) {
    suspend fun convert(history: LogEvent): AuctionActivityDto? {
        val transactionHash = history.transactionHash
        val blockHash = history.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = history.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = history.logIndex ?: DEFAULT_LOG_INDEX

        return when (val source = history.data as AuctionHistory) {
            is OnChainAuction -> {
                val (startTime, duration) = getAuctionParams(source.data)
                AuctionActivityOpenDto(
                    id = history.id.toString(),
                    date = source.date,
                    seller = source.seller,
                    sell = assetDtoConverter.convert(source.sell),
                    buy = AssetTypeDtoConverter.convert(source.buy),
                    startTime = startTime,
                    endTime = source.endTime,
                    duration = duration,
                    minimalStep = priceNormalizer.normalize(source.buy, source.minimalStep.value),
                    minimalPrice = priceNormalizer.normalize(source.buy, source.minimalPrice.value),
                    hash = source.hash,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(source.source)
                )
            }
            is BidPlaced -> {
                val buyAssetType = auctionRepository.findById(source.hash)?.buy ?: return null
                AuctionActivityBidDto(
                    id = history.id.toString(),
                    date = source.date,
                    bid = auctionBidDtoConverter.convert(buyAssetType, source.bid),
                    hash = source.hash,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(source.source)
                )
            }
            is AuctionFinished -> {
                AuctionActivityFinishDto(
                    id = history.id.toString(),
                    date = source.date,
                    hash = source.hash,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(source.source)
                )
            }
            is AuctionCancelled -> {
                AuctionActivityCancelDto(
                    id = history.id.toString(),
                    date = source.date,
                    hash = source.hash,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(source.source)
                )
            }
        }
    }

    private fun convert(source: HistorySource): AuctionActivityDto.Source {
        return when (source) {
            HistorySource.RARIBLE ->
                AuctionActivityDto.Source.RARIBLE
            HistorySource.OPEN_SEA, HistorySource.CRYPTO_PUNKS ->
                throw IllegalArgumentException("Not supported auction history source")
        }
    }

    private fun getAuctionParams(data: AuctionData): Pair<Instant?, BigInteger?> {
        return when (data) {
            is RaribleAuctionV1DataV1 ->
                data.startTime?.let { Instant.ofEpochSecond(it.value.toLong()) } to data.duration.value
        }
    }

    private companion object {
        val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
        const val DEFAULT_BLOCK_NUMBER: Long = 0
        const val DEFAULT_LOG_INDEX: Int = 0
    }
}
