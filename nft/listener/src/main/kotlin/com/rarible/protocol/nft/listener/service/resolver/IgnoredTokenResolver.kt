package com.rarible.protocol.nft.listener.service.resolver

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class IgnoredTokenResolver(
    private val listenerProperties: NftListenerProperties,
    indexerProperties: NftIndexerProperties,
    appEnv: ApplicationEnvironmentInfo,
) : AbstractResolver<Set<Address>>(indexerProperties, appEnv, "ignored") {

    private val skipTransferContracts = lazy {
        val resource = read(Ignored::class.java)?.skipTransferContracts?.map { Address.apply(it) }?.toSet() ?: emptySet()
        val properties = listenerProperties.skipTransferContracts.map { Address.apply(it) }.toSet()
        resource + properties
    }

    override fun resolve(): Set<Address> {
        return skipTransferContracts.value
    }

    private data class Ignored(val skipTransferContracts: Set<String>)
}
