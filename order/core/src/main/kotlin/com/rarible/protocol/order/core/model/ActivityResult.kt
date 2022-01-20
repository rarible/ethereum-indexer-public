package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import org.bson.types.ObjectId
import java.time.Instant

sealed class ActivityResult {
    abstract fun getId(): ObjectId
    abstract fun getDate(): Instant

    companion object {
        private val COMPARATOR = compareByDescending(ActivityResult::getDate)
            .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: ActivitySort): Comparator<ActivityResult> =
            when(sort) {
                ActivitySort.LATEST_FIRST -> COMPARATOR
                ActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
            }
    }
}

sealed class OrderActivityResult: ActivityResult() {

    data class History(val value: LogEvent): OrderActivityResult() {
        override fun getId(): ObjectId = this.value.id
        override fun getDate(): Instant = when (this.value.data) {
            is OrderExchangeHistory -> (this.value.data as OrderExchangeHistory).date
            is AuctionHistory -> (this.value.data as AuctionHistory).date
            else -> throw IllegalArgumentException("Unknown history type for activityResult")
        }
    }

    data class Version(val value: OrderVersion): OrderActivityResult() {
        override fun getId(): ObjectId = this.value.id
        override fun getDate(): Instant = value.createdAt
    }
}

sealed class AuctionActivityResult: ActivityResult() {

    data class History(val value: LogEvent): AuctionActivityResult() {
        override fun getId(): ObjectId = this.value.id
        override fun getDate(): Instant = when (this.value.data) {
            is OrderExchangeHistory -> (this.value.data as OrderExchangeHistory).date
            is AuctionHistory -> (this.value.data as AuctionHistory).date
            else -> throw IllegalArgumentException("Unknown history type for activityResult")
        }
    }

    data class OffchainHistory(val value: AuctionOffchainHistory): AuctionActivityResult() {
        override fun getId(): ObjectId = ObjectId(this.value.id)
        override fun getDate(): Instant = value.date
    }
}
