package com.rarible.protocol.dto

import scalether.domain.Address

data class OwnershipIdDto(
    val token: Address,
    val tokenId: String,
    val owner: Address
)
