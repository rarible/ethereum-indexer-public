package com.rarible.protocol.nftorder.core.data

data class Fetched<T>(
    val entity: T,
    val isFetched: Boolean
)