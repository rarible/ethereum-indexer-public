package com.rarible.protocol.nftorder.core.model

import com.rarible.protocol.dto.NftItemMetaDto

data class ExtendedItem(
    val item: Item,
    val meta: NftItemMetaDto
)
