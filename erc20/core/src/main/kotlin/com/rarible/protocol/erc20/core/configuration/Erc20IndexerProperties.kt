package com.rarible.protocol.erc20.core.configuration

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("common")
data class Erc20IndexerProperties(
    val blockchain: Blockchain,
    val kafkaReplicaSet: String
)