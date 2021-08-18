package com.rarible.protocol.gateway.configuration

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_PROTOCOL_GATEWAY_PROPERTIES = "gateway"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_GATEWAY_PROPERTIES)
data class GatewayProperties(
    val blockchain: Blockchain
)
