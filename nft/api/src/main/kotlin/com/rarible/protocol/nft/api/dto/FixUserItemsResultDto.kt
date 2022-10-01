package com.rarible.protocol.nft.api.dto

data class FixUserItemsResultDto(
    val valid: List<String>,
    val fixed: List<String>,
    val unfixed: List<String>
)
