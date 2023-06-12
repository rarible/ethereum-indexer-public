package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.api.model.DomainResolveResult
import org.springframework.stereotype.Component

@Component
class CompositeDomainResolver(
    domainResolvers: List<DomainResolver>
) {
    private val resolvers = domainResolvers
        .flatMap { resolver -> resolver.types.map { type -> type to resolver } }
        .toMap()

    suspend fun resolve(name: String): DomainResolveResult {
        val type = DomainTypeParser.parse(name) ?: EMPTY_RESULT
        return resolvers[type]
            ?.also { if (!it.isValidNane(name)) throw ValidationApiException("Invalid domain name: $name") }
            ?.resolve(name) ?: error("Unexpected null result")
    }

    companion object {
        val EMPTY_RESULT: DomainResolveResult = DomainResolveResult(null)
    }
}