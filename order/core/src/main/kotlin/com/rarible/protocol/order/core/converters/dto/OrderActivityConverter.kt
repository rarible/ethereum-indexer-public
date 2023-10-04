package com.rarible.protocol.order.core.converters.dto

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.Platform.BLUR
import com.rarible.protocol.order.core.model.Platform.CRYPTO_PUNKS
import com.rarible.protocol.order.core.model.Platform.LOOKSRARE
import com.rarible.protocol.order.core.model.Platform.OPEN_SEA
import com.rarible.protocol.order.core.model.Platform.RARIBLE
import com.rarible.protocol.order.core.model.Platform.SUDOSWAP
import com.rarible.protocol.order.core.model.Platform.X2Y2
import com.rarible.protocol.order.core.model.PoolActivityResult
import com.rarible.protocol.order.core.model.PoolCreate
import com.rarible.protocol.order.core.model.PoolNftChange
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderActivityConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter,
    private val poolHistoryRepository: PoolHistoryRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(ar: OrderActivityResult, reverted: Boolean = false): OrderActivityDto? {
        return when (ar) {
            is OrderActivityResult.History -> convertExchangeHistory(ar.value, reverted)
            is OrderActivityResult.Version -> convertVersion(ar.value, reverted)
            is PoolActivityResult.History -> convertPoolHistory(ar.value, reverted)
        }
    }

    private suspend fun convertExchangeHistory(history: ReversedEthereumLogRecord, reverted: Boolean): OrderActivityDto? {
        val transactionHash = history.transactionHash
        val blockHash = history.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = history.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = history.logIndex ?: DEFAULT_LOG_INDEX
        val data = history.data as OrderExchangeHistory

        if (data.maker == null || data.make == null || data.take == null) {
            return null
        }
        return when (data) {
            is OrderSideMatch -> {
                OrderActivityMatchDto(
                    id = history.id.toString(),
                    date = data.date,
                    left = OrderActivityMatchSideDto(
                        maker = data.maker,
                        asset = assetDtoConverter.convert(data.make),
                        hash = data.hash,
                        type = typeSideDto(data, data.make, data.take)
                    ),
                    right = OrderActivityMatchSideDto(
                        maker = data.taker,
                        asset = assetDtoConverter.convert(data.take),
                        hash = data.counterHash ?: Word.apply(ByteArray(32)),
                        type = typeSideDto(data, data.take, data.make)
                    ),
                    price = if (data.isBid()) {
                        price(data.make, data.take /* NFT */)
                    } else {
                        price(data.take, data.make /* NFT */)
                    },
                    priceUsd = data.takePriceUsd ?: data.makePriceUsd,
                    transactionHash = Word.apply(transactionHash),
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(data.source),
                    type = typeDto(data),
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt,
                    marketplaceMarker = data.marketplaceMarker,
                    counterMarketplaceMarker = data.counterMarketplaceMarker
                )
            }
            is OrderCancel -> if (data.isBid()) {
                OrderActivityCancelBidDto(
                    id = history.id.toString(),
                    hash = data.hash,
                    maker = data.maker!!,
                    make = AssetTypeDtoConverter.convert(data.make!!.type),
                    take = AssetTypeDtoConverter.convert(data.take!!.type),
                    date = data.date,
                    transactionHash = Word.apply(transactionHash),
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(data.source),
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt
                )
            } else {
                OrderActivityCancelListDto(
                    id = history.id.toString(),
                    hash = data.hash,
                    maker = data.maker!!,
                    make = AssetTypeDtoConverter.convert(data.make!!.type),
                    take = AssetTypeDtoConverter.convert(data.take!!.type),
                    date = data.date,
                    transactionHash = Word.apply(transactionHash),
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(data.source),
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt
                )
            }
            is OnChainOrder -> if (data.isBid()) {
                OrderActivityBidDto(
                    date = data.date,
                    id = history.id.toString(),
                    hash = data.hash,
                    maker = data.maker,
                    make = assetDtoConverter.convert(data.make),
                    take = assetDtoConverter.convert(data.take),
                    price = price(data.make, data.take),
                    source = convert(data.source),
                    priceUsd = data.priceUsd,
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt
                )
            } else if (data.taker != null) {
                // TODO[punk]: Sell orders (as for CryptoPunks sell orders) which are dedicated to only a concrete address (via "offer for sale to address" method call)
                // are not supported by frontend, and thus the backend should not return them.
                null
            } else {
                OrderActivityListDto(
                    date = data.date,
                    id = history.id.toString(),
                    hash = data.hash,
                    maker = data.maker,
                    make = assetDtoConverter.convert(data.make),
                    take = assetDtoConverter.convert(data.take),
                    price = price(data.take, data.make),
                    source = convert(data.source),
                    priceUsd = data.priceUsd,
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt
                )
            }
        }
    }

    private suspend fun convertPoolHistory(history: ReversedEthereumLogRecord, reverted: Boolean): OrderActivityDto? {
        val transactionHash = Word.apply(history.transactionHash)
        val blockHash = history.blockHash ?: DEFAULT_BLOCK_HASH
        val blockNumber = history.blockNumber ?: DEFAULT_BLOCK_NUMBER
        val logIndex = history.logIndex ?: DEFAULT_LOG_INDEX
        val event = history.data as PoolNftChange
        val pool = poolHistoryRepository.getPoolCreateEvent(event.hash)?.data as? PoolCreate ?: return null
        val nftAsset = Asset(Erc721AssetType(event.collection, event.tokenIds.first()), EthUInt256.ONE)

        return when (event) {
            is PoolTargetNftIn -> {
                val currencyAsset = pool.currencyAsset().copy(value = event.inputValue)
                OrderActivityMatchDto(
                    id = history.id.toString(),
                    date = event.date,
                    left = OrderActivityMatchSideDto(
                        maker = pool.data.poolAddress,
                        asset = assetDtoConverter.convert(currencyAsset),
                        hash = pool.hash,
                        type = OrderActivityMatchSideDto.Type.BID,
                    ),
                    right = OrderActivityMatchSideDto(
                        maker = event.tokenRecipient,
                        asset = assetDtoConverter.convert(nftAsset),
                        hash = Word.apply(ByteArray(32)),
                        type = OrderActivityMatchSideDto.Type.SELL
                    ),
                    price = price(currencyAsset, nftAsset /* NFT */),
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(event.source),
                    type = OrderActivityMatchDto.Type.ACCEPT_BID,
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt,
                    priceUsd = event.priceUsd,
                    marketplaceMarker = null,
                    counterMarketplaceMarker = null
                )
            }
            is PoolTargetNftOut -> {
                val currencyAsset = pool.currencyAsset().copy(value = event.outputValue)
                OrderActivityMatchDto(
                    id = history.id.toString(),
                    date = event.date,
                    left = OrderActivityMatchSideDto(
                        maker = pool.data.poolAddress,
                        asset = assetDtoConverter.convert(nftAsset),
                        hash = pool.hash,
                        type = OrderActivityMatchSideDto.Type.SELL,
                    ),
                    right = OrderActivityMatchSideDto(
                        maker = event.recipient,
                        asset = assetDtoConverter.convert(currencyAsset),
                        hash = Word.apply(ByteArray(32)),
                        type = OrderActivityMatchSideDto.Type.BID
                    ),
                    price = price(currencyAsset, nftAsset /* NFT */),
                    transactionHash = transactionHash,
                    blockHash = blockHash,
                    blockNumber = blockNumber,
                    logIndex = logIndex,
                    source = convert(event.source),
                    type = OrderActivityMatchDto.Type.SELL,
                    reverted = reverted,
                    lastUpdatedAt = history.updatedAt,
                    priceUsd = event.priceUsd,
                    marketplaceMarker = null,
                    counterMarketplaceMarker = null
                )
            }
            is PoolCreate,
            is PoolNftDeposit,
            is PoolNftWithdraw -> null
        }
    }

    private fun typeDto(orderSideMatch: OrderSideMatch): OrderActivityMatchDto.Type? {
        val isNft: (Asset) -> Boolean = { it.type.nft }
        val isAdhoc: (OrderSideMatch) -> Boolean = { it.adhoc == true }
        return when {
            isNft(orderSideMatch.make) && !isAdhoc(orderSideMatch) -> OrderActivityMatchDto.Type.SELL
            isNft(orderSideMatch.take) && !isAdhoc(orderSideMatch) -> OrderActivityMatchDto.Type.ACCEPT_BID
            isNft(orderSideMatch.make) && isAdhoc(orderSideMatch) -> OrderActivityMatchDto.Type.ACCEPT_BID
            isNft(orderSideMatch.take) && isAdhoc(orderSideMatch) -> OrderActivityMatchDto.Type.SELL
            else -> null
        }
    }

    private fun typeSideDto(orderSideMatch: OrderSideMatch, make: Asset, take: Asset): OrderActivityMatchSideDto.Type? {
        val isNft: (Asset) -> Boolean = { it.type.nft }
        val isAdhoc: (OrderSideMatch) -> Boolean = { it.adhoc == true }
        return when {
            isNft(make) && !isAdhoc(orderSideMatch) -> OrderActivityMatchSideDto.Type.SELL
            isNft(take) && !isAdhoc(orderSideMatch) -> OrderActivityMatchSideDto.Type.BID
            isNft(make) && isAdhoc(orderSideMatch) -> OrderActivityMatchSideDto.Type.BID
            isNft(take) && isAdhoc(orderSideMatch) -> OrderActivityMatchSideDto.Type.SELL
            else -> null
        }
    }

    private suspend fun convertVersion(version: OrderVersion, reverted: Boolean): OrderActivityDto {
        return when {
            version.isBid() -> OrderActivityBidDto(
                date = version.createdAt,
                id = version.id.toString(),
                hash = version.hash,
                maker = version.maker,
                make = assetDtoConverter.convert(version.make),
                take = assetDtoConverter.convert(version.take),
                price = price(version.make, version.take),
                priceUsd = version.takePriceUsd ?: version.makePriceUsd,
                source = convert(version.platform),
                reverted = reverted,
                lastUpdatedAt = version.createdAt
            )
            else -> OrderActivityListDto(
                date = version.createdAt,
                id = version.id.toString(),
                hash = version.hash,
                maker = version.maker,
                make = assetDtoConverter.convert(version.make),
                take = assetDtoConverter.convert(version.take),
                price = price(version.take, version.make),
                priceUsd = version.takePriceUsd ?: version.makePriceUsd,
                source = convert(version.platform),
                reverted = reverted,
                lastUpdatedAt = version.createdAt
            )
        }
    }

    private fun convert(source: Platform): OrderActivityDto.Source {
        return when (source) {
            RARIBLE -> OrderActivityDto.Source.RARIBLE
            OPEN_SEA -> OrderActivityDto.Source.OPEN_SEA
            CRYPTO_PUNKS -> OrderActivityDto.Source.CRYPTO_PUNKS
            X2Y2 -> OrderActivityDto.Source.X2Y2
            LOOKSRARE -> OrderActivityDto.Source.LOOKSRARE
            SUDOSWAP -> OrderActivityDto.Source.SUDOSWAP
            BLUR -> OrderActivityDto.Source.BLUR
        }
    }

    private fun convert(source: HistorySource): OrderActivityDto.Source {
        return when (source) {
            HistorySource.RARIBLE -> OrderActivityDto.Source.RARIBLE
            HistorySource.OPEN_SEA -> OrderActivityDto.Source.OPEN_SEA
            HistorySource.CRYPTO_PUNKS -> OrderActivityDto.Source.CRYPTO_PUNKS
            HistorySource.X2Y2 -> OrderActivityDto.Source.X2Y2
            HistorySource.LOOKSRARE -> OrderActivityDto.Source.LOOKSRARE
            HistorySource.SUDOSWAP -> OrderActivityDto.Source.SUDOSWAP
            HistorySource.BLUR -> OrderActivityDto.Source.BLUR
        }
    }

    private suspend fun price(left: Asset, right: Asset): BigDecimal {
        return if (right.value != EthUInt256.ZERO) {
            priceNormalizer.normalize(left) / priceNormalizer.normalize(right)
        } else BigDecimal.ZERO
    }

    private companion object {
        val DEFAULT_BLOCK_HASH: Word = Word.apply(ByteArray(32))
        const val DEFAULT_BLOCK_NUMBER: Long = 0
        const val DEFAULT_LOG_INDEX: Int = 0
    }
}
