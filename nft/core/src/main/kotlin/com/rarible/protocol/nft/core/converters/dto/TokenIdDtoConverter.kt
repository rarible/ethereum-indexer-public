package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftSignatureDto
import com.rarible.protocol.dto.NftTokenIdDto
import com.rarible.protocol.nft.core.model.SignedTokenId
import io.daonomic.rpc.domain.Binary
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object TokenIdDtoConverter : Converter<SignedTokenId, NftTokenIdDto> {
    override fun convert(source: SignedTokenId): NftTokenIdDto {
        return NftTokenIdDto(
            tokenId = source.tokenId.value,
            signature = NftSignatureDto(
                v = source.sign.v.toInt(),
                r = Binary(source.sign.r),
                s = Binary(source.sign.s)
            )
        )
    }
}
