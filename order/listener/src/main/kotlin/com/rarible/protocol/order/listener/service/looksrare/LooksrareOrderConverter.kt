package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.looksrare.client.model.v1.LooksRareOrder
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.LooksrareDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class LooksrareOrderConverter(
    private val priceUpdateService: PriceUpdateService,
) {
    suspend fun convertOrder(looksrareOrder: LooksRareOrder): OrderVersion? {
       if(looksrareOrder.tokenId == null) return null // skip collection orders

        val tokenId = looksrareOrder.tokenId!!
        val orderHash = looksrareOrder.hash
        val maker = looksrareOrder.signer
        val strategy = looksrareOrder.strategy
        val currentPrice = EthUInt256.of(looksrareOrder.price)
        val startTime = looksrareOrder.startTime
        val endTime = looksrareOrder.endTime
        val createdAt = looksrareOrder.startTime
        val signature = looksrareOrder.signature
        val currency = looksrareOrder.currencyAddress

        val data = LooksrareDataV1(
            looksrareOrder.minPercentageToAsk,
            Binary.empty()
        )

        val make = Asset(
            Erc721AssetType(looksrareOrder.collectionAddress, EthUInt256(tokenId)),
            EthUInt256.of(looksrareOrder.amount)
        )

        val take = Asset(
            if(currency == Address.ZERO()) EthAssetType
            else Erc20AssetType(currency),
            currentPrice
        )

        return OrderVersion(
            hash = orderHash,
            maker = maker,
            taker = null,
            make = make,
            take = take,
            type = OrderType.SEAPORT_V1,
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

}
