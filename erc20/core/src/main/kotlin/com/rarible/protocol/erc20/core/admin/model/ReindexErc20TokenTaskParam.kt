package com.rarible.protocol.erc20.core.admin.model

import scalether.domain.Address

class ReindexErc20TokenTaskParam(
    val descriptor: Descriptor,
    val tokens: List<Address>
) {

    enum class Descriptor {
        APPROVAL,
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL
    }

    fun toParamString(): String = "${descriptor.name}:${tokens.joinToString(",") { it.prefixed() }}"

    companion object {

        const val ADMIN_REINDEX_ERC20_TOKENS = "ADMIN_REINDEX_ERC20_TOKENS"

        fun fromParamString(param: String): ReindexErc20TokenTaskParam {
            val parts = param.split(":")
            require(parts.size == 2) { "Wrong param string" }

            return ReindexErc20TokenTaskParam(
                Descriptor.valueOf(parts[0]),
                parts[1].split(",").map { Address.apply(it) }
            )
        }
    }
}