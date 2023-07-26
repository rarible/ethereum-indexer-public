package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.LooksRareMerkleProofDto
import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.dto.OrderLooksRareDataV2Dto
import com.rarible.protocol.order.core.model.LooksrareMerkleProof
import com.rarible.protocol.order.core.model.LooksrareQuoteType
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import io.daonomic.rpc.domain.Binary

object LooksrareDataDtoConverter {
    fun convert(source: OrderLooksrareDataV1): OrderLooksRareDataV1Dto {
        return OrderLooksRareDataV1Dto(
            minPercentageToAsk = source.minPercentageToAsk,
            nonce = source.getCounterValue().value.toLong(), // TODO works now, but better to make it BigInteger
            params = source.params,
            strategy = source.strategy
        )
    }

    fun convert(source: OrderLooksrareDataV2): OrderLooksRareDataV2Dto {
        return OrderLooksRareDataV2Dto(
            additionalParameters = source.additionalParameters ?: Binary.empty(),
            globalNonce = source.counterHex.value,
            orderNonce = source.orderNonce.value,
            subsetNonce = source.subsetNonce.value,
            quoteType = convert(source.quoteType),
            strategyId = source.strategyId.value,
            merkleProof = source.merkleProof?.map { convert(it) },
            merkleRoot = source.merkleRoot
        )
    }

    private fun convert(source: LooksrareMerkleProof): LooksRareMerkleProofDto {
        return LooksRareMerkleProofDto(
            position = source.position,
            value = source.value
        )
    }

    private fun convert(source: LooksrareQuoteType): OrderLooksRareDataV2Dto.QuoteType {
        return when (source) {
            LooksrareQuoteType.ASK -> OrderLooksRareDataV2Dto.QuoteType.ASK
            LooksrareQuoteType.BID -> OrderLooksRareDataV2Dto.QuoteType.BID
        }
    }
}
