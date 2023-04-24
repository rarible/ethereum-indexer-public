package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AmmOrderDto
import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.LooksRareOrderDto
import com.rarible.protocol.dto.LooksRareV2OrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderAmmDataV1Dto
import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.dto.OrderLooksRareDataV2Dto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.OrderSeaportDataV1Dto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrderX2Y2DataDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.SeaportV1OrderDto
import com.rarible.protocol.dto.X2Y2OrderDto
import com.rarible.protocol.order.core.misc.orEmpty
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.CompositeBid
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderType.AMM
import com.rarible.protocol.order.core.model.OrderType.CRYPTO_PUNKS
import com.rarible.protocol.order.core.model.OrderType.LOOKSRARE
import com.rarible.protocol.order.core.model.OrderType.OPEN_SEA_V1
import com.rarible.protocol.order.core.model.OrderType.RARIBLE_V1
import com.rarible.protocol.order.core.model.OrderType.RARIBLE_V2
import com.rarible.protocol.order.core.model.OrderType.SEAPORT_V1
import com.rarible.protocol.order.core.model.OrderType.X2Y2
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
        val status = BidStatusConverter.convert(source.status)
        val lastUpdateAt = when (status) {
            // In case of historical orders we should NOT use actual order's lastUpdatedAt
            // since historical orders are immutable and their updatedAt should be the same as createdAt
            OrderStatusDto.HISTORICAL -> source.version.createdAt
            else -> source.order.lastUpdateAt
        }
        val id = source.order.id.toString()
        return when (order.type) {
            RARIBLE_V1 -> LegacyOrderDto(
                id = id,
                hash = source.order.hash,
                status = status,

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
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            RARIBLE_V2 -> RaribleV2OrderDto(
                id = id,
                hash = source.order.hash,
                status = status,

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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderRaribleV2DataDto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OPEN_SEA_V1 -> OpenSeaV1OrderDto(
                id = id,
                hash = source.order.hash,
                status = status,

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
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            SEAPORT_V1 -> SeaportV1OrderDto(
                id = id,
                hash = source.order.hash,
                status = status,

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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderSeaportDataV1Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            CRYPTO_PUNKS -> CryptoPunkOrderDto(
                id = id,
                hash = source.order.hash,
                status = status,

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
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            LOOKSRARE -> LooksRareOrderDto(
                id = id,
                hash = source.order.hash,
                status = status,
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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderLooksRareDataV1Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            X2Y2 -> X2Y2OrderDto(
                id = id,
                hash = source.order.hash,
                status = status,
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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderX2Y2DataDto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            AMM -> AmmOrderDto(
                id = id,
                hash = source.order.hash,
                status = status,
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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderAmmDataV1Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.LOOKSRARE_V2 -> LooksRareV2OrderDto(
                id = id,
                hash = source.order.hash,
                status = status,
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
                data = OrderDataDtoConverter.convert(source.order.data) as OrderLooksRareDataV2Dto,
                makeBalance = BigInteger.ZERO,
                lastUpdateAt = lastUpdateAt,
                end = order.end,
                start = order.start,
                priceHistory = order.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
        }
    }
}
