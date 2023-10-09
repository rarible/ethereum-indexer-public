package com.rarible.protocol.nft.core.configuration

import com.rarible.blockchain.scanner.ethereum.reduce.MetricProperties
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.kafka.Compression
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.misc.toAddressSet
import com.rarible.protocol.nft.core.model.ByteCodeMarker
import com.rarible.protocol.nft.core.model.FeatureFlags
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

const val RARIBLE_PROTOCOL_NFT_INDEXER = "common"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_NFT_INDEXER)
data class NftIndexerProperties(
    val kafkaReplicaSet: String,
    override val blockchain: Blockchain,
    override val metricRootPath: String,
    val maxPollRecords: Int = 100,
    var cryptoPunksContractAddress: String,
    var ensDomainsContractAddress: String,
    val excludedFromOpenseaMetaResolution: String = "",
    var openseaLazyMintAddress: String,
    var royaltyRegistryAddress: String,
    val daemonWorkerProperties: DaemonWorkerProperties = DaemonWorkerProperties(),
    val featureFlags: FeatureFlags = FeatureFlags(),
    val actionWorkersCount: Int = 9,
    val confirmationBlocks: Int = 12,
    val ownershipSaveBatch: Int = 20,
    val returnOnlyCacheItemMeta: Boolean = false,
    val scannerProperties: ScannerProperties = ScannerProperties(),
    val itemMeta: ItemMetaProperties = ItemMetaProperties(),
    val contractAddresses: ContractAddresses = ContractAddresses(),
    val action: ActionProperties = ActionProperties(),
    val collection: CollectionProperties = CollectionProperties(),
    val scamByteCodes: ScamByteCodeProperties = ScamByteCodeProperties(),
    val ipfs: IpfsProperties,
    val ownershipFetchBatchSize: Int = 1000,
    val reduce: ReduceProperties = ReduceProperties(),
    val compression: Compression = Compression.SNAPPY,
    val opensea: OpenseaProperties = OpenseaProperties(),
    val apiUrl: String = "",
    val cacheTimeout: Long = 315360000000,
    val requestTimeout: Long = 20000,
    val mediaMetaTimeout: Long = 10000,
    val proxyUrl: String = "",
    val followRedirect: Boolean = true,
) : MetricProperties {
    data class ActionProperties(
        val burnDelay: Duration = Duration.ofHours(24)
    )

    data class ScannerProperties(
        val skipTransferContractTokens: List<String> = emptyList(),
        val senderCreatedTokens: List<String> = emptyList()
    )

    data class ItemMetaProperties(
        val maxNameLength: Int = 1000,
        val maxDescriptionLength: Int = 10000
    )

    data class ContractAddresses(
        private val market: String = ""
    ) {

        val marketAddresses = toAddressSet(market)
    }

    data class IpfsProperties(
        val ipfsGateway: String,
        val ipfsPublicGateway: String,
        val ipfsLazyGateway: String? = null
    )

    data class CollectionProperties(
        val tokenIdInitialValue: Long = 0
    )

    data class ScamByteCodeProperties(
        val markers: List<ByteCodeMarker> = emptyList(),
        val hashCodes: Set<String> = emptySet()
    )

    data class ReduceProperties(
        val maxRevertableEventsAmount: Int = 12 * 2 // 12 blocks with 2 events per block
    )

    data class OpenseaProperties(
        val readTimeout: Int = 10000,
        val connectTimeout: Int = 3000,
        val requestTimeout: Long = 20000,
        val cacheTimeout: Long = 315360000000,
        val url: String = "",
        val apiKey: String = "",
        val proxyUrl: String = "",
    )
}
