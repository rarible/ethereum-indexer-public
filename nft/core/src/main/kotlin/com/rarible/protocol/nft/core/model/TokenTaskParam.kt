package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import kotlin.reflect.KClass

sealed class TokenTaskParam {
    abstract val tokens: List<Address>

    abstract fun toParamString(): String

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <P : TokenTaskParam> fromParamString(paramType: KClass<P>, param: String): P = when (paramType) {
            ReindexTokenItemsTaskParams::class -> ReindexTokenItemsTaskParams.fromParamString(param)
            ReduceTokenItemsTaskParams::class -> ReduceTokenItemsTaskParams.fromParamString(param)
            ReindexTokenItemRoyaltiesTaskParam::class -> ReindexTokenItemRoyaltiesTaskParam.fromParamString(param)
            else -> error("Unknown param type $paramType")
        } as P
    }
}

data class ReindexTokenItemsTaskParams(
    val standard: TokenStandard,
    override val tokens: List<Address>
) : TokenTaskParam() {

    override fun toParamString(): String =
        "${standard.name}:${tokens.joinToString(",") { it.prefixed() }}"

    companion object {
        val SUPPORTED_REINDEX_TOKEN_STANDARD: Set<TokenStandard> = setOf(TokenStandard.ERC721, TokenStandard.ERC1155)
        const val ADMIN_REINDEX_TOKEN_ITEMS = "ADMIN_REINDEX_TOKEN_ITEMS"

        fun fromParamString(param: String): ReindexTokenItemsTaskParams {
            val parts = param.split(":")
            require(parts.size == 2) { "Wrong param string" }

            return ReindexTokenItemsTaskParams(
                TokenStandard.valueOf(parts[0]),
                parts[1].split(",").map { Address.apply(it) }
            )
        }
    }
}

data class ReduceTokenItemsTaskParams(val oneToken: Address) : TokenTaskParam() {
    override val tokens: List<Address> get() = listOf(oneToken)
    override fun toParamString(): String = oneToken.prefixed()

    companion object {
        const val ADMIN_REDUCE_TOKEN_ITEMS = "ADMIN_REDUCE_TOKEN_ITEMS"

        fun fromParamString(param: String): ReduceTokenItemsTaskParams =
            ReduceTokenItemsTaskParams(Address.apply(param))
    }
}

data class ReindexTokenItemRoyaltiesTaskParam(val oneToken: Address) : TokenTaskParam() {
    override val tokens: List<Address> get() = listOf(oneToken)

    override fun toParamString(): String = oneToken.prefixed()

    companion object {
        const val ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES = "ADMIN_REINDEX_TOKEN_ITEM_ROYALTIES"

        fun fromParamString(param: String): ReindexTokenItemRoyaltiesTaskParam =
            ReindexTokenItemRoyaltiesTaskParam(Address.apply(param))
    }
}
