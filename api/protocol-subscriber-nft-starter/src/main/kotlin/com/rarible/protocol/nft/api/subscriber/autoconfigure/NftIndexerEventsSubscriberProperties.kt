package com.rarible.protocol.nft.api.subscriber.autoconfigure

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_NFT_SUBSCRIBER = "protocol.nft.subscriber"

@ConfigurationProperties(PROTOCOL_NFT_SUBSCRIBER)
@ConstructorBinding
data class NftIndexerEventsSubscriberProperties(
    val brokerReplicaSet: String,
    val blockchain: Blockchain

)
