package com.rarible.protocol.nft.migration.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("ipfs")
data class IpfsProperties(
    val uploadProxy: String,
    val gateway: String,
    val cryptoPunksImagesUrl: String
)
