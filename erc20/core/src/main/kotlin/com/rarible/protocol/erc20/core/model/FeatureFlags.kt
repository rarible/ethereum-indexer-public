package com.rarible.protocol.erc20.core.model

data class FeatureFlags(
    val skipBsonMaximumSize: Boolean = false,
    val chainBalanceUpdateEnabled: Boolean = false,
    val enableSaveAllowanceToDb: Boolean = true,
)
