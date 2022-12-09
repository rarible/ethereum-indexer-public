package com.rarible.protocol.nft.listener.service.ignored

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class IgnoredTokenResolver(
    private val listenerProperties: NftListenerProperties,
    private val properties: NftIndexerProperties,
    private val appEnv: ApplicationEnvironmentInfo,
) {
    private val embeddedSkipTransferContracts = lazy { readEmbeddedSkipTransferContracts() }

    fun resolve(): Set<Address> {
        val skipTransferContracts = listenerProperties.skipTransferContracts.map { Address.apply(it) }.toSet()
        return embeddedSkipTransferContracts.value + skipTransferContracts
    }

    private fun readEmbeddedSkipTransferContracts(): Set<Address> {
        val resource =  ClassPathResource("ignored/${appEnv.name}-${properties.blockchain.name.lowercase()}.json")
        return if (resource.exists()) {
            resource.inputStream.use { stream ->
                jacksonObjectMapper()
                    .readValue(stream, Ignored::class.java)
                    .skipTransferContracts.map {
                        Address.apply(it)
                    }.toSet()
            }
        } else {
            emptySet()
        }
    }

    private data class Ignored(val skipTransferContracts: Set<String>)
}
