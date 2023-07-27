package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.dto.ArgumentFormatException
import com.rarible.protocol.nft.api.model.DomainType

object DomainTypeParser {
    private val subDomains = DomainType.values().associateBy { it.topLevelDomain }

    fun parse(name: String): DomainType? {
        val parts = name.split(".")
        if (parts.size <= 1) {
            throw ArgumentFormatException("Invalid domain name: $name")
        }
        val type = parts.last().lowercase()
        return subDomains[type]
    }
}
