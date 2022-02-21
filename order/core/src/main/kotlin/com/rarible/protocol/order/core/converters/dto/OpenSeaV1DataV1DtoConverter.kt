package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.order.core.model.*
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OpenSeaV1DataV1DtoConverter: Converter<OrderOpenSeaV1DataV1, OrderOpenSeaV1DataV1Dto> {
    override fun convert(source: OrderOpenSeaV1DataV1): OrderOpenSeaV1DataV1Dto {
        return OrderOpenSeaV1DataV1Dto(
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
            target = source.target
        )
    }

    private fun convert(source: OpenSeaOrderSide): OrderOpenSeaV1DataV1Dto.Side {
        return when (source) {
            OpenSeaOrderSide.SELL -> OrderOpenSeaV1DataV1Dto.Side.SELL
            OpenSeaOrderSide.BUY -> OrderOpenSeaV1DataV1Dto.Side.BUY
        }
    }

    private fun convert(source: OpenSeaOrderFeeMethod): OrderOpenSeaV1DataV1Dto.FeeMethod {
        return when (source) {
            OpenSeaOrderFeeMethod.PROTOCOL_FEE -> OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE
            OpenSeaOrderFeeMethod.SPLIT_FEE -> OrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: OpenSeaOrderHowToCall): OrderOpenSeaV1DataV1Dto.HowToCall {
        return when (source) {
            OpenSeaOrderHowToCall.CALL -> OrderOpenSeaV1DataV1Dto.HowToCall.CALL
            OpenSeaOrderHowToCall.DELEGATE_CALL -> OrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL
        }
    }

    private fun convert(source: OpenSeaOrderSaleKind): OrderOpenSeaV1DataV1Dto.SaleKind {
        return when (source) {
            OpenSeaOrderSaleKind.FIXED_PRICE -> OrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE
            OpenSeaOrderSaleKind.DUTCH_AUCTION -> OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION
        }
    }
}
