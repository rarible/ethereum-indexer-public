package com.rarible.protocol.nft.listener.service.resolver

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class BluechipTokenResolver(
    indexerProperties: NftIndexerProperties,
    appEnv: ApplicationEnvironmentInfo,
) : AbstractResolver<Set<Address>>(indexerProperties, appEnv, "bluechip") {

    private val topCollections = lazy {
        read(Bluechip::class.java)?.topCollections?.map { Address.apply(it) }?.toSet() ?: emptySet()
    }

    override fun resolve(): Set<Address> {
        return topCollections.value
    }

    private data class Bluechip(
        val topCollections: Set<String>
    )
}
