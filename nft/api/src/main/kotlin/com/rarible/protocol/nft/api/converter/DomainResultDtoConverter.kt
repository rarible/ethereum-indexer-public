package com.rarible.protocol.nft.api.converter

import com.rarible.protocol.dto.DomainResolveResultDto
import com.rarible.protocol.nft.api.model.DomainResolveResult

object DomainResultDtoConverter {
    fun convert(source: DomainResolveResult): DomainResolveResultDto {
        return DomainResolveResultDto(
            registrant = source.registrant,
        )
    }
}
