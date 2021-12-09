package com.rarible.protocol.nft.core.model

abstract class BlockchainEntityEvent<T> : Comparable<BlockchainEntityEvent<T>> {
    abstract val entityId: String
    abstract val timestamp: Long
    abstract val transactionHash: String
    abstract val blockNumber: Long?
    abstract val logIndex: Int?
    abstract val minorLogIndex: Int
    abstract val status: Status

    override fun compareTo(other: BlockchainEntityEvent<T>): Int {
        return comparator.compare(this, other)
    }

    private companion object {
        val comparator: Comparator<BlockchainEntityEvent<*>> = Comparator
            .comparingLong<BlockchainEntityEvent<*>> { it.blockNumber ?: 0 }
            .thenComparingInt { it.logIndex ?: 0 }
            .thenComparingInt { it.minorLogIndex }
            .thenComparing { e1, e2 -> e1.transactionHash.compareTo(e2.transactionHash) }
    }

    enum class Status {
        PENDING,
        CONFIRMED,
        REVERTED
    }
}
