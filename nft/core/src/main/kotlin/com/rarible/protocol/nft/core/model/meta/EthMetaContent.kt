package com.rarible.protocol.nft.core.model.meta

import com.rarible.protocol.dto.MetaContentDto

data class EthMetaContent(
    val url: String,
    val representation: MetaContentDto.Representation,
    val fileName: String? = null,
    val properties: EthMetaContentProperties? = null
)
