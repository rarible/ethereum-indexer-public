package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.framework.model.Log

abstract class EthereumEntityEvent<T> : Comparable<EthereumEntityEvent<T>> {
    abstract val entityId: String
    abstract val log: EthereumLog
    val timestamp: Long get() = log.createdAt.epochSecond

    open fun invert(): T = throw IllegalArgumentException("${this.javaClass} event can't invert")

    override fun compareTo(other: EthereumEntityEvent<T>): Int {
        val o1 = this
        return when (o1.log.status) {
            Log.Status.CONFIRMED, Log.Status.REVERTED -> {
                require(other.log.status == Log.Status.CONFIRMED || other.log.status == Log.Status.REVERTED) {
                    "Can't compare $o1 and $other"
                }
                confirmBlockComparator.compare(o1, other)
            }
            Log.Status.PENDING, Log.Status.INACTIVE, Log.Status.DROPPED -> {
                require(other.log.status == Log.Status.PENDING || other.log.status == Log.Status.INACTIVE || other.log.status == Log.Status.DROPPED) {
                    "Can't compare $o1 and $other"
                }
                pendingBlockComparator.compare(o1, other)
            }
        }
    }

    private companion object {
        val confirmBlockComparator: Comparator<EthereumEntityEvent<*>> = Comparator
            .comparingLong<EthereumEntityEvent<*>> { requireNotNull(it.log.blockNumber) }
            .thenComparingInt { requireNotNull(it.log.logIndex) }
            .thenComparingInt { it.log.minorLogIndex }

        val pendingBlockComparator: Comparator<EthereumEntityEvent<*>> = Comparator
            .comparing<EthereumEntityEvent<*>, String>({ it.log.transactionHash }, { t1, t2 -> t1.compareTo(t2) })
            .thenComparing({ it.log.address.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparingInt { it.log.minorLogIndex }
    }
}
