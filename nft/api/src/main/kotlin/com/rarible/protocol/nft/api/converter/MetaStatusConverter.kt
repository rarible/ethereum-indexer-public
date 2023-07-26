package com.rarible.protocol.nft.api.converter

import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.nft.core.service.item.meta.MetaException

object MetaStatusConverter {

    fun convert(source: MetaException.Status): EthMetaStatusDto {
        return when (source) {
            MetaException.Status.Unknown -> EthMetaStatusDto.ERROR
            MetaException.Status.Timeout -> EthMetaStatusDto.TIMEOUT
            MetaException.Status.UnparseableLink -> EthMetaStatusDto.UNPARSEABLE_LINK
            MetaException.Status.UnparseableJson -> EthMetaStatusDto.UNPARSEABLE_JSON
        }
    }
}
