package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
object OrderDtoConverter : Converter<Order, OrderDto> {
    override fun convert(source: Order): OrderDto {
        return when (source.type) {
            OrderType.RARIBLE_V1 -> LegacyOrderDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                fill = source.fill.value,
                makeStock = source.makeStock.value,
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature,
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { OrderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderDataLegacyDto,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                fill = source.fill.value,
                makeStock = source.makeStock.value,
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature,
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { OrderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderRaribleV2DataV1Dto,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.OPEN_SEA_V1 -> OpenSeaV1OrderDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                fill = source.fill.value,
                makeStock = source.makeStock.value,
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = source.signature,
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                pending = source.pending.map { OrderExchangeHistoryDtoConverter.convert(it) },
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderOpenSeaV1DataV1Dto,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) }
            )
            OrderType.CRYPTO_PUNKS -> CryptoPunkOrderDto(
                maker = source.maker,
                make = AssetDtoConverter.convert(source.make),
                taker = source.taker,
                take = AssetDtoConverter.convert(source.take),
                fill = source.fill.value,
                makeStock = source.makeStock.value,
                cancelled = source.cancelled,
                start = source.start,
                end = source.end,
                salt = source.salt.value.toWord(),
                signature = source.signature,
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                hash = source.hash,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                makeBalance = BigInteger.ZERO,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                pending = source.pending.map { OrderExchangeHistoryDtoConverter.convert(it) },
                data = OrderDataDtoConverter.convert(source.data) as OrderCryptoPunksDataDto
            )
        }
    }
}
