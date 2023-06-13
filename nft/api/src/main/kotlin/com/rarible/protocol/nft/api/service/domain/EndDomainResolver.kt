package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.nft.api.model.DomainResolveResult
import com.rarible.protocol.nft.api.model.DomainType
import org.springframework.stereotype.Component
import org.web3j.ens.EnsResolver

@Component
class EndDomainResolver(
    private val ensResolver: EnsResolver
) : DomainResolver {

    override val types: List<DomainType> = listOf(DomainType.ENS)

    override fun isValidName(name: String) = EnsResolver.isValidEnsName(name)

    override suspend fun resolve(name: String): DomainResolveResult {
        val address = ensResolver.resolve(name)
        return DomainResolveResult(address)
    }
}