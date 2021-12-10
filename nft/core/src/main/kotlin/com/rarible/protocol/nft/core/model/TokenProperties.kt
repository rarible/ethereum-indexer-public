package com.rarible.protocol.nft.core.model

import scalether.domain.Address

data class TokenProperties(
    val name: String,
    val description: String?,
    val image: String?,
    val externalLink: String?,
    val feeRecipient: Address?,
    val sellerFeeBasisPoints: Int?
) {
    companion object {
        val EMPTY = TokenProperties(
            name = "Untitled",
            description = null,
            image = null,
            externalLink = null,
            feeRecipient = null,
            sellerFeeBasisPoints = null
        )
    }
}
