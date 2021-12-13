package com.rarible.protocol.nft.core.model

abstract class BlockchainEntityEvent<T> : Comparable<BlockchainEntityEvent<T>> {
    abstract val entityId: String
    abstract val timestamp: Long
    abstract val address: String
    abstract val transactionHash: String
    abstract val blockNumber: Long?
    abstract val logIndex: Int?
    abstract val minorLogIndex: Int
    abstract val status: Status

    override fun compareTo(other: BlockchainEntityEvent<T>): Int {
        return when (status) {
            Status.CONFIRMED, Status.REVERTED -> {
                require(other.status == Status.CONFIRMED || other.status == Status.REVERTED) {
                    "Can't compare $status and ${other.status}"
                }
                confirmBlockComparator.compare(this, other)
            }
            Status.PENDING, Status.INACTIVE, Status.DROPPED -> {
                require(other.status == Status.PENDING || other.status == Status.INACTIVE || other.status == Status.DROPPED) {
                    "Can't compare $status and ${other.status}"
                }
                pendingBlockComparator.compare(this, other)
            }
        }
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
        val confirmBlockComparator: Comparator<BlockchainEntityEvent<*>> = Comparator
            .comparingLong<BlockchainEntityEvent<*>> { requireNotNull(it.blockNumber) }
            .thenComparingInt { requireNotNull(it.logIndex) }
            .thenComparingInt { it.minorLogIndex }

        val pendingBlockComparator: Comparator<BlockchainEntityEvent<*>> = Comparator
            .comparing<BlockchainEntityEvent<*>, String>({ it.transactionHash }, { t1, t2 -> t1.compareTo(t2)  })
            .thenComparing({ it.address }, { a1, a2 -> a1.compareTo(a2)  })
            .thenComparingInt { it.minorLogIndex }
    }

    enum class Status {
        PENDING,
        CONFIRMED,
        INACTIVE,
        DROPPED,
        REVERTED
    }
}
