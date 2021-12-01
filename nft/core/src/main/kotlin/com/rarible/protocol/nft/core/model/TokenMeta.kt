package com.rarible.protocol.nft.core.model

import scalether.domain.Address

data class TokenMeta(
    val name: String,
    val description: String?,
    val image: String?,
    val imageMeta: MediaMeta?,
    val external_link: String?,
    val fee_recipient: Address?,
    val seller_fee_basis_points: Int?
) {
    companion object {
        val EMPTY = TokenMeta(
            name = "Untitled",
            description = null,
            image = null,
            imageMeta = null,
            external_link = null,
            fee_recipient = null,
            seller_fee_basis_points = null
        )
    }
}
