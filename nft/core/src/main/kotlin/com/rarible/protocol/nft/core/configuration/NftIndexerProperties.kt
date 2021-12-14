package com.rarible.protocol.nft.core.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.model.ReduceVersion
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

const val RARIBLE_PROTOCOL_NFT_INDEXER = "common"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_NFT_INDEXER)
data class NftIndexerProperties(
    val kafkaReplicaSet: String,
    val blockchain: Blockchain,
    val maxPollRecords: Int = 100,
    var cryptoPunksContractAddress: String,
    var openseaLazyMintAddress: String,
    var royaltyRegistryAddress: String,
    val factory: FactoryAddresses,
    val nftItemMetaExtenderWorkersCount: Int = 4,
    val daemonWorkerProperties: DaemonWorkerProperties = DaemonWorkerProperties(),
    val featureFlags: FeatureFlags = FeatureFlags(),
    val confirmationBlocks: Int = 12
) {
    data class FactoryAddresses(
        val erc721Rarible: String,
        val erc721RaribleUser: String,
        val erc1155Rarible: String,
        val erc1155RaribleUser: String
    )

    data class FeatureFlags(
        val reduceVersion: ReduceVersion = ReduceVersion.V1,
        var isRoyaltyServiceEnabled: Boolean = true,
        var ownershipBatchHandle: Boolean = false
    )
}
