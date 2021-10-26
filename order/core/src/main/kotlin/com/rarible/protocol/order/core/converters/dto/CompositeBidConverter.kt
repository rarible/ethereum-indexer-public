package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.order.core.misc.orEmpty
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.CompositeBid
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class CompositeBidConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter
) {
    suspend fun convert(source: CompositeBid): OrderDto {
        val order = source.order
        return when (order.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderDto(
                hash = source.order.hash,
                status = BidStatusConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePrice = source.version.makePrice,
                takePrice = source.version.takePrice,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                fillValue = priceNormalizer.normalize(source.order.take.type, source.order.fill.value),
                makeStock = source.order.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.order.make.type, source.order.makeStock.value),
                cancelled = source.order.cancelled,
                salt = source.order.salt.value.toWord(),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderDataLegacyDto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = source.order.lastUpdateAt
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderDto(
                hash = source.order.hash,
                status = BidStatusConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePrice = source.version.makePrice,
                takePrice = source.version.takePrice,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                fillValue = priceNormalizer.normalize(source.order.take.type, source.order.fill.value),
                makeStock = source.order.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.order.make.type, source.order.makeStock.value),
                cancelled = source.order.cancelled,
                salt = source.order.salt.value.toWord(),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderRaribleV2DataV1Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = source.order.lastUpdateAt
            )
            OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderDto(
                hash = source.order.hash,
                status = BidStatusConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePrice = source.version.makePrice,
                takePrice = source.version.takePrice,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                fillValue = priceNormalizer.normalize(source.order.take.type, source.order.fill.value),
                makeStock = source.order.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.order.make.type, source.order.makeStock.value),
                cancelled = source.order.cancelled,
                salt = source.order.salt.value.toWord(),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderOpenSeaV1DataV1Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = source.order.lastUpdateAt
            )
            OrderType.CRYPTO_PUNKS -> CryptoPunkOrderDto(
                hash = source.order.hash,
                status = BidStatusConverter.convert(source.status),

                make = assetDtoConverter.convert(source.version.make),
                take = assetDtoConverter.convert(source.version.take),
                maker = source.version.maker,
                taker = source.version.taker,
                makePrice = source.version.makePrice,
                takePrice = source.version.takePrice,
                makePriceUsd = source.version.makePriceUsd,
                takePriceUsd = source.version.takePriceUsd,
                createdAt = source.version.createdAt,

                fill = source.order.fill.value,
                fillValue = priceNormalizer.normalize(source.order.take.type, source.order.fill.value),
                makeStock = source.order.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.order.make.type, source.order.makeStock.value),
                cancelled = source.order.cancelled,
                salt = source.order.salt.value.toWord(),
                signature = source.order.signature.orEmpty(),
                data = OrderDataDtoConverter.convert(source.order.data) as OrderCryptoPunksDataDto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = source.order.lastUpdateAt
            )
        }
    }
}
