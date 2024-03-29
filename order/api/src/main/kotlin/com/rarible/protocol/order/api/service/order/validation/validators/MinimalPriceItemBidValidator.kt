package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.NftCollectionAssetType
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.floor.FloorSellService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal

@Component
class MinimalPriceItemBidValidator(
    private val floorSellService: FloorSellService,
    private val priceUpdateService: PriceUpdateService,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    private val bidValidation: OrderIndexerProperties.BidValidationProperties,
) : OrderFormValidator {

    override suspend fun validate(form: OrderForm) {
        if (form.make.type.nft) return
        if (featureFlags.checkMinimalBidPrice.not()) return

        val nftAsset = form.take.type
        if (featureFlags.checkMinimalCollectionBidPriceOnly) {
            if (nftAsset !is CollectionAssetType) return
        }
        val prices = priceUpdateService.getAssetsUsdValue(take = form.take, make = form.make, at = nowMillis())
        val takePriceUsd = prices?.takePriceUsd ?: throw OrderUpdateException(
            "Can't determine 'takePriceUsd', maybe not supported currency: ${form.make.type.token}",
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_PRICE
        )
        return when (nftAsset) {
            is NftCollectionAssetType -> validateWithCollectionFloorPrice(nftAsset.token, takePriceUsd)
            is Erc20AssetType,
            is EthAssetType,
            is GenerativeArtAssetType -> {
            }
        }
    }

    private suspend fun validateWithCollectionFloorPrice(token: Address, takePriceUsd: BigDecimal) {
        val floorPriceUsd = floorSellService.getFloorSellPriceUsd(token)
        logger.info("Get floor usd price $floorPriceUsd for collection $token")
        if (floorPriceUsd == null) {
            validateMinimumPrice(takePriceUsd)
        } else if (floorPriceUsd >= takePriceUsd) {
            validateWithFloorPrice(token, takePriceUsd, floorPriceUsd)
        }
    }

    private fun validateWithFloorPrice(token: Address, price: BigDecimal, floorPriceUsd: BigDecimal) {
        val minimumFromFloorPrice = floorPriceUsd * bidValidation.minPercentFromFloorPrice
        if (price < minimumFromFloorPrice) {
            logger.error(
                "Can't set bid for {}, price={}, minimum={}",
                token, price, minimumFromFloorPrice
            )
            throw OrderUpdateException(
                "Order has invalid bid price. Price should be not less 0.75% from floor price ($floorPriceUsd)",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_PRICE
            )
        }
    }

    private fun validateMinimumPrice(price: BigDecimal) {
        if (price < bidValidation.minPriceUsd) {
            throw OrderUpdateException(
                "Order has invalid price. Price should be not less 1USD",
                EthereumOrderUpdateApiErrorDto.Code.INCORRECT_PRICE
            )
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(MinimalPriceItemBidValidator::class.java)
    }
}
