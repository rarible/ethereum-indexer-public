package com.rarible.protocol.nft.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

internal const val RARIBLE_PROTOCOL_LISTENER_STORAGE = "api"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_LISTENER_STORAGE)
data class NftIndexerApiProperties(
    val chainId: Long,
    @NestedConfigurationProperty
    val operator: OperatorProperties,
    val skipHeavyRequest: Boolean = false,
    val metaSyncLoadingTimeout: Long = 10000
) {
    data class OperatorProperties(
        val privateKey: String
    )
}
