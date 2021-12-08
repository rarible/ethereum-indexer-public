package com.rarible.protocol.nft.core.model

abstract class BlockchainEntityEvent<T> : Comparable<BlockchainEntityEvent<T>> {
    abstract val entityId: String
    abstract val blockNumber: Long
    abstract val logIndex: Int
    abstract val status: Status

    override fun compareTo(other: BlockchainEntityEvent<T>): Int {
        return comparator.compare(this, other)
    }

    private companion object {
        val comparator: Comparator<BlockchainEntityEvent<*>> = Comparator
            .comparingLong<BlockchainEntityEvent<*>> { it.blockNumber }
            .thenComparingInt { it.logIndex }
    }

    enum class Status {
        PENDING,
        CONFIRMED,
        REVERTED
    }
}
