package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.misc.looksrareError
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger

@Component
class LooksrareOrderConverter(
    private val priceUpdateService: PriceUpdateService,
    private val tokenStandardProvider: TokenStandardProvider,
    private val currencyAddresses: OrderIndexerProperties.CurrencyContractAddresses,
    private val looksrareErrorCounter: RegisteredCounter
) {
    suspend fun convert(looksrareOrder: LooksrareOrder): OrderVersion? {
        if (
            looksrareOrder.tokenId == null ||
            looksrareOrder.signature == null ||
            looksrareOrder.nonce > MAX_NONCE
        ) return run {
            looksrareErrorCounter.increment()
            null
        }
        val tokenId = looksrareOrder.tokenId!!
        val orderHash = looksrareOrder.hash
        val maker = looksrareOrder.signer
        val currentPrice = EthUInt256.of(looksrareOrder.price)
        val startTime = looksrareOrder.startTime
        val endTime = looksrareOrder.endTime
        val createdAt = looksrareOrder.startTime
        val signature = looksrareOrder.signature
        val collectionAddress = looksrareOrder.collectionAddress
        val collectionStandard = tokenStandardProvider.getTokenStandard(collectionAddress) ?: run {
            logger.looksrareError("Can't get collection $collectionAddress standard")
            looksrareErrorCounter.increment()
            return null
        }
        val currencyAddress = looksrareOrder.currencyAddress

        val data = OrderLooksrareDataV1(
            minPercentageToAsk = looksrareOrder.minPercentageToAsk,
            params = if (looksrareOrder.params?.length() == 0) null else looksrareOrder.params,
            counterHex = EthUInt256(looksrareOrder.nonce),
            strategy = looksrareOrder.strategy
        )
        val (make, take) = kotlin.run {
            val nft = Asset(
                when (collectionStandard) {
                    TokenStandard.ERC721 -> Erc721AssetType(collectionAddress, EthUInt256(tokenId))
                    TokenStandard.ERC1155 -> Erc1155AssetType(collectionAddress, EthUInt256(tokenId))
                },
                EthUInt256.of(looksrareOrder.amount)
            )
            val currency = Asset(
                if (currencyAddress == Address.ZERO() || currencyAddress == currencyAddresses.weth) EthAssetType
                else Erc20AssetType(currencyAddress),
                currentPrice
            )
            if (looksrareOrder.isOrderAsk) (nft to currency) else (currency to nft)
        }
        return OrderVersion(
            hash = orderHash,
            maker = maker,
            taker = null,
            make = make,
            take = take,
            type = OrderType.LOOKSRARE,
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

    private companion object {
        val MAX_NONCE: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    }
}
