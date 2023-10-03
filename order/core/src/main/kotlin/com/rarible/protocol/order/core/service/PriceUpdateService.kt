package com.rarible.protocol.order.core.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.model.OrderVersion
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EXT)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class PriceUpdateService(
    private val blockchain: Blockchain,
    private val currencyApi: CurrencyControllerApi,
    private val priceNormalizer: PriceNormalizer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getAssetUsdValue(assetType: AssetType, value: BigInteger, at: Instant): BigDecimal? {
        val asset = Asset(assetType, EthUInt256.of(value))
        val normalized = priceNormalizer.normalize(asset)
        val usdRate = getAssetPrice(asset, at) ?: return null
        return usdValue(usdRate, normalized)
    }

    suspend fun getAssetValue(assetType: AssetType, value: BigInteger): BigDecimal {
        return priceNormalizer.normalize(assetType, value)
    }

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

    suspend fun withUpdatedAllPrices(orderVersion: OrderVersion): OrderVersion {
        return withUpdatedUsdPrices(withUpdatedPrices(orderVersion))
    }

    suspend fun withUpdatedAllPrices(order: Order): Order {
        return withUpdatedUsdPrices(withUpdatedPrices(order))
    }

    suspend fun getTokenRate(token: Address, at: Instant): BigDecimal? {
        val rate = currencyApi.getCurrencyRate(convert(blockchain), token.hex(), at.toEpochMilli()).awaitFirstOrNull()?.rate
        if (rate == null) {
            logger.warn("Currency api didn't respond any value for $blockchain: $token at $at")
        }
        return rate
    }

    private suspend fun withUpdatedUsdPrices(order: Order): Order {
        val usdValue = getAssetsUsdValue(order.make, order.take, nowMillis()) ?: return order
        return order.withOrderUsdValue(usdValue)
    }

    private suspend fun withUpdatedUsdPrices(orderVersion: OrderVersion): OrderVersion {
        val usdValue = getAssetsUsdValue(orderVersion.make, orderVersion.take, nowMillis()) ?: return orderVersion
        return orderVersion.withOrderUsdValue(usdValue)
    }

    suspend fun withUpdatedPrices(orderVersion: OrderVersion): OrderVersion {
        val normalizedMake = priceNormalizer.normalize(orderVersion.make)
        val normalizedTake = priceNormalizer.normalize(orderVersion.take)
        return when {
            orderVersion.make.type.nft -> orderVersion.copy(makePrice = updatePrice(normalizedTake, normalizedMake, orderVersion))
            orderVersion.take.type.nft -> orderVersion.copy(takePrice = updatePrice(normalizedMake, normalizedTake, orderVersion))
            else -> orderVersion
        }
    }

    suspend fun withUpdatedPrices(order: Order): Order {
        val normalizedMake = priceNormalizer.normalize(order.make)
        val normalizedTake = priceNormalizer.normalize(order.take)
        return when {
            order.make.type.nft -> order.copy(makePrice = updatePrice(normalizedTake, normalizedMake, order))
            order.take.type.nft -> order.copy(takePrice = updatePrice(normalizedMake, normalizedTake, order))
            else -> order
        }
    }

    private fun updatePrice(divisible: BigDecimal, divider: BigDecimal, order: Any): BigDecimal {
        return try {
            divisible.divide(divider)
        } catch (e: ArithmeticException) {
            logger.warn("makePrice or takePrice field may have the wrong value, order: $order")
            divisible / divider
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
            getTokenRate(address, at)
        }
    }

    private fun usdPrice(usdRate: BigDecimal, nftPart: BigDecimal, payingPart: BigDecimal): BigDecimal {
        if (nftPart.signum() == 0) {
            return BigDecimal.ZERO
        }
        return usdRate * payingPart / nftPart
    }

    private fun usdValue(usdRate: BigDecimal, payingPart: BigDecimal): BigDecimal {
        return usdRate * payingPart
    }

    private fun convert(source: Blockchain): BlockchainDto {
        return when (source) {
            Blockchain.ETHEREUM -> BlockchainDto.ETHEREUM
            Blockchain.POLYGON -> BlockchainDto.POLYGON
            Blockchain.OPTIMISM -> BlockchainDto.OPTIMISM
            Blockchain.MANTLE -> BlockchainDto.POLYGON // TODO: Fix it
        }
    }
}
