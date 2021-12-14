package com.rarible.protocol.nft.core.model

import com.rarible.protocol.nft.core.model.ReduceVersion

data class FeatureFlags(
    var reduceVersion: ReduceVersion = ReduceVersion.V1,
    var isRoyaltyServiceEnabled: Boolean = true,
    var ownershipBatchHandle: Boolean = false
)
