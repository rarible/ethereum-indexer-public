package com.rarible.protocol.nft.core.model

data class FeatureFlags(
    var scannerVersion: ScannerVersion = ScannerVersion.V2,
    var isRoyaltyServiceEnabled: Boolean = true,
    var validateCreatorByTransactionSender: Boolean = false,
    val internalMetaTopicBatchHandle: Boolean = false,
    val internalMetaTopicBatchSize: Int = 100,
    val enableMetaRawPropertiesCache: Boolean = true,
    val enableProxyForMetaDownload: Boolean = false,
    val enableNonStandardCollections: Boolean = false,
    @Deprecated("Need remove this flag in release 1.31")
    val pendingDisabled: Boolean = true,
    val enableTokenMetaSelfRepair: Boolean = false,
    val filterScamToken: Boolean = false,
    val saveTokenByteCode: Boolean = true,
    val compactRevertableEvents: Boolean = false,
)
