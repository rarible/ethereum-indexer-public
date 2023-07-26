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
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.orEmpty
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.isMakeFillOrder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PriceNormalizer
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger

@Component
class OrderDtoConverter(
    private val priceNormalizer: PriceNormalizer,
    private val assetDtoConverter: AssetDtoConverter,
    private val properties: OrderIndexerProperties,
    private val commonSigner: CommonSigner,
) {
    suspend fun convert(source: Order): OrderDto {
        val orderStatus = OrderStatusDtoConverter.convert(source.status)
        val id = source.id.toString()
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
                signature = convertSignature(orderStatus, source.signature),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderDataLegacyDto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = orderStatus,
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
            )
            OrderType.RARIBLE_V2 -> RaribleV2OrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = convertFillValue(source),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = convertSignature(orderStatus, source.signature),
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderRaribleV2DataDto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = orderStatus,
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
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
                signature = if (properties.featureFlags.hideOpenSeaSignatures) Binary.apply() else when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
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
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
            )
            OrderType.SEAPORT_V1 -> SeaportV1OrderDto(
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
                signature = if (properties.featureFlags.hideOpenSeaSignatures) Binary.apply() else when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderSeaportDataV1Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
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
                signature = when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
                hash = source.hash,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                makeBalance = BigInteger.ZERO,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                status = orderStatus,
                data = OrderDataDtoConverter.convert(source.data) as OrderCryptoPunksDataDto,
                optionalRoyalties = source.isOptionalRoyalties(),
            )

            OrderType.X2Y2 -> X2Y2OrderDto(
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
                signature = when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                id = id,
                hash = source.hash,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                makeBalance = BigInteger.ZERO,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = null,
                takePriceUsd = null,
                pending = null,
                status = orderStatus,
                data = OrderDataDtoConverter.convert(source.data) as OrderX2Y2DataDto,
                optionalRoyalties = source.isOptionalRoyalties(),
            )
            OrderType.LOOKSRARE -> LooksRareOrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = convertFillValue(source),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = if (properties.featureFlags.hideOpenSeaSignatures) Binary.apply() else when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                pending = null,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderLooksRareDataV1Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
            )
            OrderType.LOOKSRARE_V2 -> LooksRareV2OrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = convertFillValue(source),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = if (properties.featureFlags.hideOpenSeaSignatures) Binary.apply() else when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                pending = null,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderLooksRareDataV2Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
            )
            OrderType.AMM -> AmmOrderDto(
                maker = source.maker,
                make = assetDtoConverter.convert(source.make),
                taker = source.taker,
                take = assetDtoConverter.convert(source.take),
                fill = source.fill.value,
                fillValue = convertFillValue(source),
                makeStock = source.makeStock.value,
                makeStockValue = priceNormalizer.normalize(source.make.type, source.makeStock.value),
                cancelled = source.cancelled,
                salt = source.salt.value.toWord(),
                signature = if (properties.featureFlags.hideOpenSeaSignatures) Binary.apply() else when (orderStatus) {
                    OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
                    else -> source.signature.orEmpty()
                },
                createdAt = source.createdAt,
                lastUpdateAt = source.lastUpdateAt,
                dbUpdatedAt = source.dbUpdatedAt,
                pending = null,
                id = id,
                hash = source.hash,
                data = OrderDataDtoConverter.convert(source.data) as OrderAmmDataV1Dto,
                makePrice = source.makePrice,
                takePrice = source.takePrice,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd,
                makeBalance = BigInteger.ZERO,
                status = OrderStatusDtoConverter.convert(source.status),
                start = source.start,
                end = source.end,
                priceHistory = source.priceHistory.map { OrderPriceHistoryDtoConverter.convert(it) },
                optionalRoyalties = source.isOptionalRoyalties(),
            )
        }
    }

    private fun convertSignature(status: OrderStatusDto, signature: Binary?): Binary? {
        return when (status) {
            OrderStatusDto.INACTIVE, OrderStatusDto.CANCELLED -> null
            else -> signature.orEmpty()
        }?.let { commonSigner.fixSignature(it) }
    }

    private suspend fun convertFillValue(source: Order): BigDecimal {
        return if (source.isMakeFillOrder) {
            priceNormalizer.normalize(source.make.type, source.fill.value)
        } else {
            priceNormalizer.normalize(source.take.type, source.fill.value)
        }
    }
}
