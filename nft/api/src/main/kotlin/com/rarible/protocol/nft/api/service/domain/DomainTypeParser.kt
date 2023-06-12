package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.nft.api.model.DomainType

object DomainTypeParser {
    private val subDomains = DomainType.values().associateBy { it.topLevelDomain }

    fun parse(name: String): DomainType? {
        val parts = name.split(".")
        if (parts.isEmpty()) {
            throw IllegalArgumentException("Invalid domain name: $name")
        }
        val type = parts.last().lowercase()
        return subDomains[type]
    }
}