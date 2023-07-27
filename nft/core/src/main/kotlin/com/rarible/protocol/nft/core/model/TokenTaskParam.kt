package com.rarible.protocol.nft.core.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import scalether.domain.Address
import kotlin.reflect.KClass

sealed class TokenTaskParam {
    abstract val tokens: List<Address>

    abstract fun toParamString(): String

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <P : TokenTaskParam> fromParamString(paramType: KClass<P>, param: String): P = when (paramType) {
            ReindexTokenItemsTaskParams::class -> ReindexTokenItemsTaskParams.fromParamString(param)
            ReindexTokenItemRoyaltiesTaskParam::class -> ReindexTokenItemRoyaltiesTaskParam.fromParamString(param)
            ReindexTokenTaskParams::class -> ReindexTokenTaskParams.fromParamString(param)
            ReindexCryptoPunksTaskParam::class -> ReindexCryptoPunksTaskParam.fromParamString(param)

            ReduceTokenTaskParams::class -> ReduceTokenTaskParams.fromParamString(param)
            ReduceTokenItemsTaskParams::class -> ReduceTokenItemsTaskParams.fromParamString(param)
            else -> error("Unknown param type $paramType")
        } as P
    }
}

data class ReindexTokenTaskParams(
    override val tokens: List<Address>
) : TokenTaskParam() {

    override fun toParamString(): String = tokens.joinToString(",") { it.prefixed() }

    companion object {
        const val ADMIN_REINDEX_TOKEN = "ADMIN_REINDEX_TOKEN"

        fun fromParamString(param: String): ReindexTokenTaskParams =
            ReindexTokenTaskParams(param.split(",").map { Address.apply(param) })
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

data class ReduceTokenTaskParams(val oneToken: Address) : TokenTaskParam() {
    override val tokens: List<Address> get() = listOf(oneToken)
    override fun toParamString(): String = oneToken.prefixed()

    companion object {
        const val ADMIN_REDUCE_TOKEN = "ADMIN_REDUCE_TOKEN"

        fun fromParamString(param: String): ReduceTokenTaskParams =
            ReduceTokenTaskParams(Address.apply(param))
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

data class ReindexCryptoPunksTaskParam(val event: PunkEvent, val from: Long) : TokenTaskParam() {
    override val tokens: List<Address> get() = emptyList()

    override fun toParamString(): String = "${event.name}:$from"

    companion object {
        const val ADMIN_REINDEX_CRYPTO_PUNKS = "ADMIN_REINDEX_CRYPTO_PUNKS"

        fun fromParamString(param: String): ReindexCryptoPunksTaskParam {
            val parts = param.split(":")
            require(parts.size == 2) { "Wrong param string" }

            return ReindexCryptoPunksTaskParam(
                event = PunkEvent.valueOf(parts[0]),
                from = parts[1].toLong()
            )
        }
    }

    enum class PunkEvent {
        TRANSFER,
        BOUGHT,
        ASSIGN
    }
}

data class ReduceTokenRangeTaskParams(
    // This param originally is not needed, but we can use it in mongo queries
    // In case of manually created tasks we can omit it
    val parent: String?,
    val from: String,
    val to: String
) {

    fun toParamString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {

        const val ADMIN_REDUCE_TOKEN_RANGE = "ADMIN_REDUCE_TOKEN_RANGE"

        private val mapper = ObjectMapper().registerKotlinModule()

        fun parse(param: String): ReduceTokenRangeTaskParams {
            return mapper.readValue(param, ReduceTokenRangeTaskParams::class.java)
        }
    }
}

data class ReduceTokenItemsDependentTaskParams(
    val address: Address,
    val dependency: String
) {

    fun toParamString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {

        const val REDUCE_TOKEN_ITEMS_DEPENDENT = "REDUCE_TOKEN_ITEMS_DEPENDENT"

        private val mapper = ObjectMapper()
            .registerKotlinModule()

        fun parse(param: String): ReduceTokenItemsDependentTaskParams {
            return mapper.readValue(param, ReduceTokenItemsDependentTaskParams::class.java)
        }
    }
}
