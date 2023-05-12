package com.rarible.protocol.erc20.core.model

data class FeatureFlags(
    var reduceVersion: ReduceVersion = ReduceVersion.V2,
    var scannerVersion: ScannerVersion = ScannerVersion.V2,
    val skipBsonMaximumSize: Boolean = false
)
