package com.rarible.protocol.nft.core.configuration

import com.rarible.ethereum.domain.Blockchain
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

const val RARIBLE_PROTOCOL_NFT_INDEXER = "common"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_NFT_INDEXER)
data class NftIndexerProperties(
    val kafkaReplicaSet: String,
    val blockchain: Blockchain,
    var cryptoPunksContractAddress: String,
    var openseaLazyMintAddress: String
)
