package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.x2y2.client.model.ErcType
import com.rarible.x2y2.client.model.Order
import com.rarible.x2y2.client.model.OrderStatus
import org.springframework.stereotype.Component
import scalether.domain.Address
import com.rarible.x2y2.client.model.OrderType as ClientOrderType

@Component
class X2Y2OrderConverter(
    private val priceUpdateService: PriceUpdateService,
    private val metrics: ForeignOrderMetrics
) {
    suspend fun convert(order: Order): OrderVersion? {
        if (order.isBundle) return run {
            metrics.onDownloadedOrderSkipped(Platform.X2Y2, "bundle")
            null
        }
        if (order.isCollectionOffer) return run {
            metrics.onDownloadedOrderSkipped(Platform.X2Y2, "collection_offer")
            null
        }
        if (order.status != OrderStatus.OPEN) return run {
            metrics.onDownloadedOrderSkipped(Platform.X2Y2, "closed")
            null
        }
        if (order.type != ClientOrderType.SELL) return run {
            metrics.onDownloadedOrderSkipped(Platform.X2Y2, "bid")
            null
        }
        val token = order.token ?: return run {
            metrics.onDownloadedOrderSkipped(Platform.X2Y2, "empty_token")
            null
        }
        val data = OrderX2Y2DataV1(
            itemHash = order.itemHash,
            isCollectionOffer = order.isCollectionOffer,
            isBundle = order.isBundle,
            side = order.side,
            orderId = order.id
        )
        val (make, take) = run {
            val nft = Asset(
                type = when (token.ercType) {
                    ErcType.ERC721 -> Erc721AssetType(token.contract, EthUInt256(token.tokenId!!))
                    ErcType.ERC1155 -> Erc1155AssetType(token.contract, EthUInt256(token.tokenId!!))
                },
                value = EthUInt256(order.amount)
            )
            val currency = Asset(
                type = if (order.currency == Address.ZERO()) {
                    EthAssetType
                } else {
                    Erc20AssetType(token = order.currency)
                },
                value = EthUInt256.of(order.price)
            )
            nft to currency
        }
        return OrderVersion(
            maker = order.maker,
            make = make,
            take = take,
            taker = null,
            type = OrderType.X2Y2,
            platform = Platform.X2Y2,
            start = order.createdAt.epochSecond,
            end = order.endAt.epochSecond,
            createdAt = order.createdAt,
            data = data,
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            salt = EthUInt256.ZERO,
            signature = null,
            hash = order.itemHash
        ).let {
            priceUpdateService.withUpdatedPrices(it)
        }
    }
}
