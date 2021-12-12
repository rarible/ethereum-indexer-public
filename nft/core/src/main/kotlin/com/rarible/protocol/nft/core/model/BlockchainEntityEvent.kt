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

    val isConfirmed: Boolean
        get() = status == Status.CONFIRMED

    val isReverted: Boolean
        get() = status == Status.REVERTED

    val isPending: Boolean
        get() = status == Status.PENDING

    val isInactive: Boolean
        get() = status == Status.INACTIVE

    private companion object {
        val comparator: Comparator<BlockchainEntityEvent<*>> = Comparator
            .comparingLong<BlockchainEntityEvent<*>> { requireNotNull(it.blockNumber) }
            .thenComparingInt { requireNotNull(it.logIndex) }
            .thenComparingInt { it.minorLogIndex }
    }

    enum class Status {
        PENDING,
        CONFIRMED,
        INACTIVE,
        REVERTED
    }
}
