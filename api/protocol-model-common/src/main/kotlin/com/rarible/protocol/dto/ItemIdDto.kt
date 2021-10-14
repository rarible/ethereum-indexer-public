package com.rarible.protocol.dto

import scalether.domain.Address

data class ItemIdDto(
    val token: Address,
    val tokenId: String
)
