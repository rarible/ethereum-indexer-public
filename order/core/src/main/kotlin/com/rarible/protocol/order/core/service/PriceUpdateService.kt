package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.order.core.model.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@Service
class PriceUpdateService(
    private val blockchain: Blockchain,
    private val currencyApi: CurrencyControllerApi,
    private val priceNormalizer: PriceNormalizer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getAssetsUsdValue(make: Asset, take: Asset, at: Instant): OrderUsdValue? {
        val normalizedMake = priceNormalizer.normalize(make)
        val normalizedTake = priceNormalizer.normalize(take)

        return when {
            make.type.nft -> {
                val usdRate = getAssetPrice(take, at) ?: return null

                val usdValue = usdValue(usdRate, normalizedTake)
                val usdPrice = usdPrice(usdRate, normalizedMake, normalizedTake)

                OrderUsdValue.SellOrder(
                    makePriceUsd = usdPrice,
                    takeUsd = usdValue
                )
            }
            take.type.nft -> {
                val usdRate = getAssetPrice(make, at) ?: return null

                val usdValue = usdValue(usdRate, normalizedMake)
                val usdPrice = usdPrice(usdRate, normalizedTake, normalizedMake)

                OrderUsdValue.BidOrder(
                    makeUsd = usdValue,
                    takePriceUsd = usdPrice
                )
            }
            else -> null
        }
    }

    suspend fun withUpdatedUsdPrices(orderVersion: OrderVersion): OrderVersion {
        val usdValue = getAssetsUsdValue(orderVersion.make, orderVersion.take, nowMillis()) ?: return orderVersion
        return orderVersion.copy(
            makeUsd = usdValue.makeUsd,
            takeUsd = usdValue.takeUsd,
            makePriceUsd = usdValue.makePriceUsd,
            takePriceUsd = usdValue.takePriceUsd
        )
    }

    /**
     * The price is USD rate of paying value multiplied by ratio of normalized paying value to normalized nft value
     * E.g. make is 1 NFT, take is 1.5 ETH, 1ETH = $2000. Price is 1.5ETH / 1NFT * $2000 = $3000
     */
    suspend fun updateOrderPrice(order: Order, at: Instant): Order {
        logger.info("Try update prices")
        val normalizedMake = priceNormalizer.normalize(order.make)
        val normalizedTake = priceNormalizer.normalize(order.take)

        return when {
            order.make.type.nft -> {
                val usdRate = getAssetPrice(order.take, at)
                val usdPrice = usdRate?.let { usdPrice(it, normalizedMake, normalizedTake) }
                if (usdPrice != null) order.withTakePrice(usdPrice) else order
            }
            order.take.type.nft -> {
                val usdRate = getAssetPrice(order.make, at)
                val usdPrice = usdRate?.let { usdPrice(it, normalizedTake, normalizedMake) }
                if (usdPrice != null) order.withMakePrice(usdPrice) else order
            }
            else -> order
        }
    }

    private suspend fun getAssetPrice(asset: Asset, at: Instant): BigDecimal? {
        val address = when (val assetType = asset.type) {
            is Erc20AssetType -> assetType.token
            is EthAssetType -> Address.ZERO()
            else -> null
        }
        return if (address == null) {
            null
        } else {
            val rate =
                currencyApi.getCurrencyRate(blockchain.name, address.hex(), at.toEpochMilli()).awaitFirstOrNull()?.rate
            if (rate == null) {
                logger.warn("Currency api didn't respond any value")
            }
            rate
        }
    }

    private fun usdPrice(usdRate: BigDecimal, nftPart: BigDecimal, payingPart: BigDecimal): BigDecimal {
        return usdRate * payingPart / nftPart
    }

    private fun usdValue(usdRate: BigDecimal, payingPart: BigDecimal): BigDecimal {
        return usdRate * payingPart
    }
}
