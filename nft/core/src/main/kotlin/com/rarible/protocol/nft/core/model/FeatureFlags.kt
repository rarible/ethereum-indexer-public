package com.rarible.protocol.nft.core.model

data class FeatureFlags(
    var scannerVersion: ScannerVersion = ScannerVersion.V2,
    var isRoyaltyServiceEnabled: Boolean = true,
    var validateCreatorByTransactionSender: Boolean = false,
    var firstMinterIsCreator: Boolean = true,
    val internalMetaTopicBatchHandle: Boolean = false,
    val internalMetaTopicBatchSize: Int = 100,
    val enableMetaRawPropertiesCache: Boolean = true,
    val enableProxyForMetaDownload: Boolean = false,
    val enableNonStandardCollections: Boolean = false,
    @Deprecated("Need remove this flag in release 1.31")
    val pendingDisabled: Boolean = true,
    val enableTokenMetaSelfRepair: Boolean = false,
    val filterScamToken: Boolean = false,
    val detectScamToken: Boolean = true,
    var detectScamTokenThreshold: Int = 1000,
    val saveTokenByteCode: Boolean = true,
    val compactRevertableEvents: Boolean = false,
)
