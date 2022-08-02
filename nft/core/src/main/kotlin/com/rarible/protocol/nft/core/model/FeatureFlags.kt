package com.rarible.protocol.nft.core.model

data class FeatureFlags(
    var reduceVersion: ReduceVersion = ReduceVersion.V2,
    var scannerVersion: ScannerVersion = ScannerVersion.V2,
    var isRoyaltyServiceEnabled: Boolean = true,
    var validateCreatorByTransactionSender: Boolean = false,
    val internalMetaTopicBatchHandle: Boolean = false,
    val internalMetaTopicBatchSize: Int = 100,
    val enableMetaRawPropertiesCache: Boolean = true,
    val enableProxyForMetaDownload: Boolean = false,
    @Deprecated("Need remove this flag in release 1.31")
    val pendingDisabled: Boolean = true
)