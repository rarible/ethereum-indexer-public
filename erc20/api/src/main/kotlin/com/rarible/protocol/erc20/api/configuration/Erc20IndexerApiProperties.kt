package com.rarible.protocol.erc20.api.configuration

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_PROTOCOL_ERC20_API_PROPERTIES = "api"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_ERC20_API_PROPERTIES)
data class Erc20IndexerApiProperties(
    val blockchain: Blockchain
)
