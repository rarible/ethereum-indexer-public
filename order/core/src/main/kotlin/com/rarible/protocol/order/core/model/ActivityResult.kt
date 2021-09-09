package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.repository.sort.OrderActivitySort
import org.bson.types.ObjectId
import java.time.Instant

sealed class ActivityResult {
    abstract fun getId(): ObjectId
    abstract fun getDate(): Instant

    data class History(val value: LogEvent): ActivityResult() {
        override fun getId(): ObjectId = this.value.id
        override fun getDate(): Instant = (this.value.data as OrderExchangeHistory).date
    }

    data class Version(val value: OrderVersion): ActivityResult() {
        override fun getId(): ObjectId = this.value.id
        override fun getDate(): Instant = value.createdAt
    }

    companion object {
        private val COMPARATOR = compareByDescending(ActivityResult::getDate)
            .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: OrderActivitySort): Comparator<ActivityResult> =
            when(sort) {
                OrderActivitySort.LATEST_FIRST -> COMPARATOR
                OrderActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
            }
    }
}
