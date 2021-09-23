package com.rarible.protocol.nft.api.dto

import scalether.domain.Address

data class TokenDto(
    val id: Address,
    val standard: String,
    val owner: Address?,
    val name: String,
    val symbol: String?,
    val features: List<String>
)
