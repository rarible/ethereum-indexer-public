package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus

abstract class EthereumEntityEvent<T> : Comparable<EthereumEntityEvent<T>> {
    abstract val entityId: String
    abstract val log: EthereumLog
    val timestamp: Long get() = log.createdAt.epochSecond

    open fun invert(): T = throw IllegalArgumentException("${this.javaClass} event can't invert")

    override fun compareTo(other: EthereumEntityEvent<T>): Int {
        val o1 = this
        return when (o1.log.status) {
            EthereumLogStatus.CONFIRMED, EthereumLogStatus.REVERTED -> {
                require(other.log.status == EthereumLogStatus.CONFIRMED || other.log.status == EthereumLogStatus.REVERTED) {
                    "Can't compare $o1 and $other"
                }
                confirmBlockComparator.compare(o1, other)
            }
            EthereumLogStatus.PENDING, EthereumLogStatus.INACTIVE, EthereumLogStatus.DROPPED -> {
                if (other.log.status == EthereumLogStatus.CONFIRMED) {
                    eventKeyComparator.compare(o1, other)
                } else {
                    require(
                        other.log.status == EthereumLogStatus.PENDING
                                || other.log.status == EthereumLogStatus.INACTIVE
                                || other.log.status == EthereumLogStatus.DROPPED
                    ) {
                        "Can't compare $o1 and $other"
                    }
                    pendingBlockComparator.compare(o1, other)
                }
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
            .thenComparing({ it.log.topic.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparingInt { it.log.minorLogIndex }

        val eventKeyComparator: Comparator<EthereumEntityEvent<*>> = Comparator
            .comparing<EthereumEntityEvent<*>, String>({ it.log.transactionHash }, { t1, t2 -> t1.compareTo(t2) })
            .thenComparing({ it.log.address.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparing({ it.log.topic.toString() }, { a1, a2 -> a1.compareTo(a2) })
    }
}
