package com.rarible.protocol.erc20.core.model

data class FeatureFlags(
    val skipBsonMaximumSize: Boolean = false,
    val chainBalanceUpdateEnabled: Boolean = true,
    val enableSaveAllowanceToDb: Boolean = true,
    val enableSaveHistoryToDb: Boolean = true,
)
