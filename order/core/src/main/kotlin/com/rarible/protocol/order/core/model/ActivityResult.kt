package com.rarible.protocol.order.core.model

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import java.time.Instant

sealed class ActivityResult {
    abstract fun getId(): String
    abstract fun getDate(): Instant
    abstract fun getUpdatedAt(): Instant

    companion object {
        private val COMPARATOR = compareByDescending(ActivityResult::getDate)
            .then(compareByDescending(ActivityResult::getId))

        private val SYNC_COMPARATOR = compareByDescending(ActivityResult::getUpdatedAt)
            .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: ActivitySort): Comparator<ActivityResult> =
            when (sort) {
                ActivitySort.LATEST_FIRST -> COMPARATOR
                ActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
                ActivitySort.SYNC_LATEST_FIRST -> SYNC_COMPARATOR
                ActivitySort.SYNC_EARLIEST_FIRST -> SYNC_COMPARATOR.reversed()
                ActivitySort.BY_ID -> compareByDescending(ActivityResult::getId)
            }
    }
}

sealed class OrderActivityResult : ActivityResult() {

    data class History(val value: ReversedEthereumLogRecord) : OrderActivityResult() {

        override fun getId(): String = this.value.id
        override fun getDate(): Instant = when (this.value.data) {
            is OrderExchangeHistory -> (this.value.data as OrderExchangeHistory).date
            is AuctionHistory -> (this.value.data as AuctionHistory).date
            else -> throw IllegalArgumentException("Unknown history type for activityResult")
        }
        override fun getUpdatedAt(): Instant = this.value.updatedAt
    }

    data class Version(val value: OrderVersion) : OrderActivityResult() {
        override fun getId(): String = this.value.id.toHexString()
        override fun getDate(): Instant = value.createdAt
        override fun getUpdatedAt(): Instant = this.value.createdAt
    }
}

sealed class PoolActivityResult : OrderActivityResult() {
    data class History(val value: ReversedEthereumLogRecord) : PoolActivityResult() {
        override fun getId(): String = this.value.id
        override fun getDate(): Instant = when (this.value.data) {
            is PoolTargetNftIn -> (this.value.data as PoolTargetNftIn).date
            is PoolTargetNftOut -> (this.value.data as PoolTargetNftOut).date
            else -> throw IllegalArgumentException("Unknown history type for activityResult")
        }
        override fun getUpdatedAt(): Instant = this.value.updatedAt
    }
}

sealed class AuctionActivityResult : ActivityResult() {

    data class History(val value: ReversedEthereumLogRecord) : AuctionActivityResult() {
        override fun getId(): String = this.value.id
        override fun getDate(): Instant = when (this.value.data) {
            is OrderExchangeHistory -> (this.value.data as OrderExchangeHistory).date
            is AuctionHistory -> (this.value.data as AuctionHistory).date
            else -> throw IllegalArgumentException("Unknown history type for activityResult")
        }
        override fun getUpdatedAt(): Instant = this.value.updatedAt
    }

    data class OffchainHistory(val value: AuctionOffchainHistory) : AuctionActivityResult() {
        override fun getId(): String = this.value.id
        override fun getDate(): Instant = value.date
        override fun getUpdatedAt(): Instant = this.value.createdAt
    }
}
