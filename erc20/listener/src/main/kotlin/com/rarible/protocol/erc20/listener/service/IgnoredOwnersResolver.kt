package com.rarible.protocol.erc20.listener.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class IgnoredOwnersResolver(
    private val properties: Erc20ListenerProperties,
    private val appEnv: ApplicationEnvironmentInfo,
) {
    fun resolve(): Set<Address> {
        val resource = ClassPathResource("ignored-owners/${appEnv.name}-${properties.blockchain.name.lowercase()}.json")
        return if (resource.exists()) {
            resource.inputStream.use { stream ->
                jacksonObjectMapper().readValue(stream, Owners::class.java).ignored.map {
                    Address.apply(it)
                }.toSet()
            }
        } else {
            emptySet()
        }
    }

    internal data class Owners(val ignored: List<String>)
}
