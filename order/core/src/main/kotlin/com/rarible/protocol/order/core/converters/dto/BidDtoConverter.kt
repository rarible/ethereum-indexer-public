package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.CryptoPunksOrderBidDto
import com.rarible.protocol.dto.LegacyOrderBidDto
import com.rarible.protocol.dto.OpenSeaV1OrderBidDto
import com.rarible.protocol.dto.OrderBidDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.RaribleV2OrderBidDto
import com.rarible.protocol.order.core.misc.orEmpty
import com.rarible.protocol.order.core.model.CompositeBid
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.stereotype.Component
import scalether.abi.Uint256Type
import java.math.BigInteger

@Component
class BidDtoConverter(
    private val assetDtoConverter: AssetDtoConverter
) {
    suspend fun convert(source: CompositeBid): OrderBidDto {
        val order = source.order
        return when (order.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderBidDto(
                orderHash = source.order.hash,
                status = BidStatusDtoConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                makeStock = source.order.makeStock.value,
                cancelled = source.order.cancelled,
                salt = Uint256Type.encode(source.order.salt.value),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderDataLegacyDto,
                makeBalance = BigInteger.ZERO
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderBidDto(
                orderHash = source.order.hash,
                status = BidStatusDtoConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                makeStock = source.order.makeStock.value,
                cancelled = source.order.cancelled,
                salt = Uint256Type.encode(source.order.salt.value),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderRaribleV2DataDto,
                makeBalance = BigInteger.ZERO
            )
            OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderBidDto(
                orderHash = source.order.hash,
                status = BidStatusDtoConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                makeStock = source.order.makeStock.value,
                cancelled = source.order.cancelled,
                salt = Uint256Type.encode(source.order.salt.value),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderOpenSeaV1DataV1Dto,
                makeBalance = BigInteger.ZERO
            )
            OrderType.CRYPTO_PUNKS -> CryptoPunksOrderBidDto(
                orderHash = source.order.hash,
                status = BidStatusDtoConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                makeStock = source.order.makeStock.value,
                cancelled = source.order.cancelled,
                salt = Uint256Type.encode(source.order.salt.value),
                signature = source.order.signature.orEmpty(),
                makeBalance = BigInteger.ZERO
            )
        }
    }
}
