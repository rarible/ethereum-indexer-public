package com.rarible.protocol.nft.core.model

data class FeatureFlags(
    var isRoyaltyServiceEnabled: Boolean = true,
    var validateCreatorByTransactionSender: Boolean = false,
    var firstMinterIsCreator: Boolean = true,
    val enableMetaRawPropertiesCache: Boolean = true,
    val enableProxyForMetaDownload: Boolean = true,
    val enableNonStandardCollections: Boolean = false,
    val filterScamToken: Boolean = false,
    val detectScamToken: Boolean = true,
    var detectScamTokenThreshold: Int = 100,
    val saveTokenByteCode: Boolean = true,
    val compactRevertableEvents: Boolean = true,
)
