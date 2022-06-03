package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import java.time.Instant

data class TokenProperties(
    val name: String,
    val description: String?,
    val image: String?,
    val externalLink: String?,
    val feeRecipient: Address?,
    val sellerFeeBasisPoints: Int?,

    val createdAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val rights: String? = null,
    val rightsUri: String? = null,
    val externalUri: String? = null,
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
