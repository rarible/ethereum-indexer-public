package com.rarible.protocol.order.core.converters.dto

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.AuctionActivityBidDto
import com.rarible.protocol.dto.AuctionActivityCancelDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.AuctionActivityFinishDto
import com.rarible.protocol.dto.AuctionActivityOpenDto
import com.rarible.protocol.order.core.model.AuctionCancelled
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OnChainAuction
import com.rarible.protocol.order.core.repository.auction.AuctionRepository
import io.daonomic.rpc.domain.Word
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuctionActivityConverter(
    private val auctionDtoConverter: AuctionDtoConverter,
    private val auctionBidDtoConverter: AuctionBidDtoConverter,
    private val auctionRepository: AuctionRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(history: LogEvent): AuctionActivityDto? {
        val transactionHash = history.transactionHash
        val blockHash = history.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = history.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = history.logIndex ?: DEFAULT_LOG_INDEX

        val auctionHistory = history.data as AuctionHistory
        val auction = auctionRepository.findById(auctionHistory.hash)
        if (auction == null) {
            logger.warn("Auction with hash {} not found for LogEvent {}", auctionHistory.hash, history)
            return null
        }

        val auctionDto = auctionDtoConverter.convert(auction)
        val source = convert(auctionHistory.source)

        return when (auctionHistory) {
            is OnChainAuction -> {
                AuctionActivityOpenDto(
                    id = history.id.toString(),
                    date = auctionHistory.date,
                    source = source,
                    auction = auctionDto,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex
                )
            }
            is BidPlaced -> {
                AuctionActivityBidDto(
                    id = history.id.toString(),
                    date = auctionHistory.date,
                    source = source,
                    auction = auctionDto,
                    bid = auctionBidDtoConverter.convert(auction.buy, auctionHistory.buyer, auctionHistory.bid),
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                )
            }
            is AuctionFinished -> {
                AuctionActivityFinishDto(
                    id = history.id.toString(),
                    date = auctionHistory.date,
                    source = source,
                    auction = auctionDto,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex
                )
            }
            is AuctionCancelled -> {
                AuctionActivityCancelDto(
                    id = history.id.toString(),
                    date = auctionHistory.date,
                    source = source,
                    auction = auctionDto,
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex
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

    private companion object {
        val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
        const val DEFAULT_BLOCK_NUMBER: Long = 0
        const val DEFAULT_LOG_INDEX: Int = 0
    }
}
