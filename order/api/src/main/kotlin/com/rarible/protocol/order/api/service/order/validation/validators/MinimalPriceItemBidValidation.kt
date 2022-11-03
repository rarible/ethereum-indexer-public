package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.api.service.order.validation.OrderVersionValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.NftCollectionAssetType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.order.logger
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.floor.FloorSellService
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal

@Component
class MinimalPriceItemBidValidation(
    private val floorSellService: FloorSellService,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    private val bidValidation: OrderIndexerProperties.BidValidationProperties
) : OrderVersionValidator {

    override suspend fun validate(orderVersion: OrderVersion) {
        if (featureFlags.checkMinimalBidPrice.not()) return

        val nftAsset = orderVersion.take.type
        val takePriceUsd = orderVersion.takePriceUsd ?: throw OrderUpdateException(
            "Can't determine 'takePriceUsd', maybe not supported currency: ${orderVersion.make.type.token}",
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_PRICE
        )
        return when (nftAsset) {
            is NftCollectionAssetType -> validateWithCollectionFloorPrice(nftAsset.token, takePriceUsd)
            is Erc20AssetType,
            is EthAssetType,
            is GenerativeArtAssetType -> {}
        }
    }

    private suspend fun validateWithCollectionFloorPrice(token: Address, takePriceUsd: BigDecimal) {
        val floorPriceUsd = floorSellService.getFloorSellPriceUsd(token)
        logger.info("Get floor usd price $floorPriceUsd for collection $token")
        if (floorPriceUsd == null) {
            validateMinimumPrice(takePriceUsd)
        } else if (floorPriceUsd >= takePriceUsd)  {
            validateWithFloorPrice(takePriceUsd, floorPriceUsd)
        }
    }

    private fun validateWithFloorPrice(price: BigDecimal, floorPriceUsd: BigDecimal) {
        val percentFromFloorPrice = price.divide(floorPriceUsd)
        if (percentFromFloorPrice < bidValidation.minPercentFromFloorPrice) {
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
}