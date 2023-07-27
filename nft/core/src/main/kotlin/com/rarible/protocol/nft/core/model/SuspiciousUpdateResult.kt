package com.rarible.protocol.nft.core.model

sealed class SuspiciousUpdateResult {
    abstract val next: UpdateSuspiciousItemsState.Asset

    data class Success(override val next: UpdateSuspiciousItemsState.Asset) : SuspiciousUpdateResult()

    data class Fail(override val next: UpdateSuspiciousItemsState.Asset) : SuspiciousUpdateResult()
}
