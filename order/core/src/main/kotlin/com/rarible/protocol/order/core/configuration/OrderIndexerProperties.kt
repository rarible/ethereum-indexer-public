package com.rarible.protocol.order.core.configuration

import com.rarible.ethereum.domain.Blockchain
import io.daonomic.rpc.domain.Binary
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty
import scalether.domain.Address

const val RARIBLE_PROTOCOL_ORDER_INDEXER = "common"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_PROTOCOL_ORDER_INDEXER)
data class OrderIndexerProperties(
    val blockchain: Blockchain,
    val kafkaReplicaSet: String,
    val eip712DomainName: String,
    val eip712DomainVersion: String,
    val chainId: Int,
    val operatorPrivateKey: Binary,
    val protocolCommission: Int,
    @NestedConfigurationProperty
    val exchangeContractAddresses: ExchangeContractAddresses,
    @NestedConfigurationProperty
    val transferProxyAddresses: TransferProxyAddresses,
    @NestedConfigurationProperty
    val publish: PublishProperties = PublishProperties(),
    @NestedConfigurationProperty
    val featureFlags: FeatureFlags = FeatureFlags()
) {
    data class ExchangeContractAddresses(
        val v1: Address,
        val v1Old: Address? = null,
        val v2: Address,
        val openSeaV1: Address,
        var cryptoPunks: Address
    )

    data class TransferProxyAddresses(
        val transferProxy: Address,
        val erc20TransferProxy: Address,
        val erc721LazyTransferProxy: Address,
        val erc1155LazyTransferProxy: Address,
        var cryptoPunksTransferProxy: Address
    )

    data class PublishProperties(
        val publishOpenSeaOrdersToCommonTopic: Boolean = false
    )

    data class FeatureFlags(
        val useCommonTransactionTraceProvider: Boolean = true,
        val showAllOrdersByDefault: Boolean = false
    )
}

