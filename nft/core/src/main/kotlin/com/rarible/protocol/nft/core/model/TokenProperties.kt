package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import scalether.domain.Address
import java.time.Instant

data class TokenProperties(
    val name: String,
    val description: String?,
    @Deprecated("Should be replaced by content")
    val image: String?,
    val externalUri: String?,
    val feeRecipient: Address?,
    val sellerFeeBasisPoints: Int?,

    val createdAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val rights: String? = null,
    val rightsUri: String? = null,
    val tokenUri: String? = null,
    val content: List<EthMetaContent> = emptyList()
) {
    companion object {
        val EMPTY = TokenProperties(
            name = "Untitled",
            description = null,
            image = null,
            externalUri = null,
            feeRecipient = null,
            sellerFeeBasisPoints = null
        )
    }
}
