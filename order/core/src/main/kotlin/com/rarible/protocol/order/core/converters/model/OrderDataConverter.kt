package com.rarible.protocol.order.core.converters.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.dto.OrderDataDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV2Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.OrderX2Y2DataDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderDataConverter : Converter<OrderDataDto, OrderData> {
    override fun convert(source: OrderDataDto): OrderData {
        return when (source) {
            is OrderRaribleV2DataV1Dto -> {
                val payouts = convert(source.payouts)
                val originFees = convert(source.originFees)
                OrderRaribleV2DataV1(payouts = payouts, originFees = originFees)
            }
            is OrderRaribleV2DataV2Dto -> {
                val payouts = convert(source.payouts)
                val originFees = convert(source.originFees)
                OrderRaribleV2DataV2(payouts = payouts, originFees = originFees, isMakeFill = source.isMakeFill)
            }
            is OrderRaribleV2DataV3SellDto -> OrderRaribleV2DataV3Sell(
                payout = source.payout?.let { convert(it) },
                originFeeFirst = source.originFeeFirst?.let { convert(it) },
                originFeeSecond = source.originFeeSecond?.let { convert(it) },
                maxFeesBasePoint = EthUInt256.of(source.maxFeesBasePoint),
                marketplaceMarker = source.marketplaceMarker
            )
            is OrderRaribleV2DataV3BuyDto -> OrderRaribleV2DataV3Buy(
                payout = source.payout?.let { convert(it) },
                originFeeFirst = source.originFeeFirst?.let { convert(it) },
                originFeeSecond = source.originFeeSecond?.let { convert(it) },
                marketplaceMarker = source.marketplaceMarker
            )
            is OrderDataLegacyDto -> {
                OrderDataLegacy(source.fee)
            }
            is OrderOpenSeaV1DataV1Dto -> OrderOpenSeaV1DataV1(
                exchange = source.exchange,
                makerRelayerFee = source.makerRelayerFee,
                takerRelayerFee = source.takerRelayerFee,
                makerProtocolFee = source.makerProtocolFee,
                takerProtocolFee = source.takerProtocolFee,
                feeRecipient = source.feeRecipient,
                feeMethod = convert(source.feeMethod),
                side = convert(source.side),
                saleKind = convert(source.saleKind),
                howToCall = convert(source.howToCall),
                callData = source.callData,
                replacementPattern = source.replacementPattern,
                staticTarget = source.staticTarget,
                staticExtraData = source.staticExtraData,
                extra = source.extra,
                target = source.target,
                nonce = null
            )
            is OrderCryptoPunksDataDto,
            is OrderBasicSeaportDataV1Dto,
            is OrderLooksRareDataV1Dto,
            is OrderX2Y2DataDto,
            is OrderSudoSwapAmmDataV1Dto -> throw IllegalArgumentException("Not supported converting  order data")
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.Side): OpenSeaOrderSide {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.Side.SELL -> OpenSeaOrderSide.SELL
            OrderOpenSeaV1DataV1Dto.Side.BUY -> OpenSeaOrderSide.BUY
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.FeeMethod): OpenSeaOrderFeeMethod {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE -> OpenSeaOrderFeeMethod.PROTOCOL_FEE
            OrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE -> OpenSeaOrderFeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.HowToCall): OpenSeaOrderHowToCall {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.HowToCall.CALL -> OpenSeaOrderHowToCall.CALL
            OrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL -> OpenSeaOrderHowToCall.DELEGATE_CALL
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.SaleKind): OpenSeaOrderSaleKind {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE -> OpenSeaOrderSaleKind.FIXED_PRICE
            OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION -> OpenSeaOrderSaleKind.DUTCH_AUCTION
        }
    }

    private fun convert(source: List<PartDto>): List<Part> {
        return source.map { PartConverter.convert(it) }
    }

    private fun convert(source: PartDto): Part {
        return PartConverter.convert(source)
    }
}
