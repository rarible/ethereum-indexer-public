package com.rarible.protocol.nft.api.dto

import com.rarible.protocol.nft.api.model.ItemProblemType

data class CheckUserItemsResultDto(
    val valid: List<String>,
    val invalid: Map<String, ItemProblemType>,
)


