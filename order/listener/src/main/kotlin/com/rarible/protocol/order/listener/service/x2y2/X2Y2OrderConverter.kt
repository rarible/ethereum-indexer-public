package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.x2y2.client.model.Order
import java.math.BigInteger
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class X2Y2OrderConverter(
    private val priceUpdateService: PriceUpdateService
) {

    suspend fun convertOrder(order: Order): OrderVersion {

        val data = OrderX2Y2DataV1(
            itemHash = order.itemHash,
            isCollectionOffer = order.isCollectionOffer,
            isBundle = order.isBundle,
            side = order.side
        )

        return OrderVersion(
            maker = order.maker,
            make = Asset(
                type = Erc721AssetType(token = order.token!!.contract, tokenId = EthUInt256(order.token!!.tokenId!!)),
                value = EthUInt256(BigInteger.ONE)
            ),
            take = Asset(
                type = if (order.currency == Address.ZERO()) {
                    EthAssetType
                } else {
                    Erc20AssetType(token = order.currency)
                },
                value = EthUInt256.of(order.price)
            ),
            taker = null,
            type = OrderType.X2Y2,
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
            salt = EthUInt256(order.id),
            signature = null, // TODO realize if need
            hash = order.itemHash
        ).let {
            priceUpdateService.withUpdatedPrices(it)
        }

    }
}

