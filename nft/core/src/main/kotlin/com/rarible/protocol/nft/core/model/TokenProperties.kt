package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import scalether.domain.Address
import java.time.Instant

data class TokenProperties(
    val name: String,
    val description: String?,
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
    val content: TokenMetaContent = TokenMetaContent()
) {

    companion object {

        val EMPTY = TokenProperties(
            name = "Untitled",
            description = null,
            externalUri = null,
            feeRecipient = null,
            sellerFeeBasisPoints = null
        )
    }

    fun isEmpty(): Boolean {
        return this == EMPTY
    }
}

data class TokenMetaContent(
    val imageOriginal: EthMetaContent? = null
) {
    fun asList(): List<EthMetaContent> {
        val content = ArrayList<EthMetaContent>(1)
        this.imageOriginal?.let { content.add(it) }
        return content
    }
}
