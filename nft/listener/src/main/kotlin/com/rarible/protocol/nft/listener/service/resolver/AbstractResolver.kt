package com.rarible.protocol.nft.listener.service.resolver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import org.springframework.core.io.ClassPathResource

abstract class AbstractResolver<T>(
    private val properties: NftIndexerProperties,
    private val appEnv: ApplicationEnvironmentInfo,
    private val resource: String
) {
    abstract fun resolve(): T

    protected fun <R> read(type: Class<R>): R? {
        val resource = ClassPathResource("$resource/${appEnv.name}-${properties.blockchain.name.lowercase()}.json")
        return if (resource.exists()) {
            resource.inputStream.use { stream ->
                mapper.readValue(stream, type)
            }
        } else {
           null
        }
    }

    private companion object {
        val mapper = jacksonObjectMapper().registerKotlinModule()
    }
}
