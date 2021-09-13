package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
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
                extra = source.extra
            )
            is OrderCryptoPunksDataDto -> OrderCryptoPunksData
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
}
