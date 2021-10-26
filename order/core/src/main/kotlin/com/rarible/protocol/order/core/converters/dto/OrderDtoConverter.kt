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
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.PriceNormalizer
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OrderDtoConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter,
    private val orderExchangeHistoryDtoConverter: OrderExchangeHistoryDtoConverter
) {

    suspend fun convert(source: Order): OrderDto {
        return when (source.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = priceNormalizer.normalize(source.take.type, source.fill.value),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature.orEmpty(),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { orderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderDataLegacyDto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = priceNormalizer.normalize(source.take.type, source.fill.value),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature.orEmpty(),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { orderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderRaribleV2DataV1Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = priceNormalizer.normalize(source.take.type, source.fill.value),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature.orEmpty(),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { orderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderOpenSeaV1DataV1Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.CRYPTO_PUNKS -> CryptoPunkOrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = priceNormalizer.normalize(source.take.type, source.fill.value),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                start = source.start,
                end = source.end,
                salt = source.salt.value.toWord(),
                signature = source.signature.orEmpty(),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                hash = source.hash,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                makeBalance = BigInteger.ZERO,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                pending = source.pending.map { orderExchangeHistoryDtoConverter.convert(it) },
                status = OrderStatusDtoConverter.convert(source.status),
                data = OrderDataDtoConverter.convert(source.data) as OrderCryptoPunksDataDto
            )
        }
    }
}
