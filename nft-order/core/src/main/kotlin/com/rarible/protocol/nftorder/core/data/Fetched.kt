package com.rarible.protocol.nftorder.core.data

data class Fetched<E, O>(
    val entity: E,
    val original: O?
) {
    fun isFetched(): Boolean {
        return original != null
    }
}

