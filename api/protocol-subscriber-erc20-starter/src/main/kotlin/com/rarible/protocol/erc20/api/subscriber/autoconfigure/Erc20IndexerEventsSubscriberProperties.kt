package com.rarible.protocol.erc20.api.subscriber.autoconfigure

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_ERC20_SUBSCRIBER = "protocol.erc20.subscriber"

@ConfigurationProperties(PROTOCOL_ERC20_SUBSCRIBER)
@ConstructorBinding
data class Erc20IndexerEventsSubscriberProperties(
    val brokerReplicaSet: String,
    val blockchain: Blockchain
)
