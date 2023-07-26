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
        val type = DomainTypeParser.parse(name)
        return resolvers[type]
            ?.also { if (!it.isValidName(name)) throw ValidationApiException("Invalid domain name: $name") }
            ?.resolve(name)
            ?: throw ValidationApiException("Top level domain is not supported")
    }
}
