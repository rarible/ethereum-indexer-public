package com.rarible.protocol.nft.core.model

@Deprecated("Ethereum API will soon be not responsible for loading content meta. ")
data class ContentMeta(
    val type: String,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null
)
