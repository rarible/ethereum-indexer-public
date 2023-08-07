package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.looksrare.client.model.v2.CollectionType
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.MerkleProof
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.LooksrareMerkleProof
import com.rarible.protocol.order.core.model.LooksrareQuoteType
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class LooksrareOrderConverter(
    private val priceUpdateService: PriceUpdateService,
    private val currencyAddresses: OrderIndexerProperties.CurrencyContractAddresses,
    private val metrics: ForeignOrderMetrics
) {
    suspend fun convert(looksrareOrder: LooksrareOrder): OrderVersion? {
        if (
            looksrareOrder.itemIds.size != 1 ||
            looksrareOrder.amounts.size != 1
        ) return run {
            metrics.onDownloadedOrderError(Platform.LOOKSRARE, "incorrect_amount")
            null
        }
        val tokenId = looksrareOrder.itemIds.single()
        val orderHash = looksrareOrder.hash
        val maker = looksrareOrder.signer
        val currentPrice = EthUInt256.of(looksrareOrder.price)
        val startTime = looksrareOrder.startTime
        val endTime = looksrareOrder.endTime
        val createdAt = looksrareOrder.startTime
        val signature = looksrareOrder.signature
        val collectionAddress = looksrareOrder.collection
        val currencyAddress = looksrareOrder.currency
        val collectionType = looksrareOrder.collectionType

        val data = OrderLooksrareDataV2(
            quoteType = convert(looksrareOrder.quoteType),
            counterHex = EthUInt256.of(looksrareOrder.globalNonce),
            orderNonce = EthUInt256.of(looksrareOrder.orderNonce),
            subsetNonce = EthUInt256.of(looksrareOrder.subsetNonce),
            strategyId = EthUInt256.of(looksrareOrder.strategyId),
            additionalParameters = looksrareOrder.additionalParameters,
            merkleRoot = looksrareOrder.merkleRoot,
            merkleProof = looksrareOrder.merkleProof?.map { convert(it) }
        )
        val (make, take) = kotlin.run {
            val nft = Asset(
                when (collectionType) {
                    CollectionType.ERC721 -> Erc721AssetType(collectionAddress, EthUInt256(tokenId))
                    CollectionType.ERC1155 -> Erc1155AssetType(collectionAddress, EthUInt256(tokenId))
                },
                EthUInt256.of(looksrareOrder.amounts.single())
            )
            val currency = Asset(
                if (currencyAddress == Address.ZERO() || currencyAddress == currencyAddresses.weth) EthAssetType
                else Erc20AssetType(currencyAddress),
                currentPrice
            )
            if (looksrareOrder.quoteType == QuoteType.ASK) (nft to currency) else (currency to nft)
        }
        return OrderVersion(
            hash = orderHash,
            maker = maker,
            taker = null,
            make = make,
            take = take,
            type = OrderType.LOOKSRARE_V2,
            salt = EthUInt256.ZERO,
            start = startTime.epochSecond,
            end = endTime.epochSecond,
            data = data,
            createdAt = createdAt,
            signature = signature,
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.LOOKSRARE
        ).let {
            priceUpdateService.withUpdatedPrices(it)
        }
    }

    private fun convert(source: MerkleProof): LooksrareMerkleProof = LooksrareMerkleProof(
        position = source.position,
        value = source.value
    )

    private fun convert(quoteType: QuoteType): LooksrareQuoteType = when (quoteType) {
        QuoteType.ASK -> LooksrareQuoteType.ASK
        QuoteType.BID -> LooksrareQuoteType.BID
    }
}
