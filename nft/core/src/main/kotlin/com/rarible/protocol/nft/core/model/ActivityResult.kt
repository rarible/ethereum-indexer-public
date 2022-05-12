package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.repository.history.ActivitySort
import org.bson.types.ObjectId
import java.time.Instant

data class ActivityResult(val value: LogEvent) {
    fun getId(): ObjectId = this.value.id

    fun getDate(): Instant = (this.value.data as ItemHistory).date

    fun syncData(): Instant = this.value.updatedAt

    companion object {
        private val COMPARATOR =
            compareByDescending(ActivityResult::getDate)
                .then(compareByDescending(ActivityResult::getId))

        private val SYNC_COMPARATOR = compareByDescending(ActivityResult::syncData)
            .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: ActivitySort): Comparator<ActivityResult> =
            when (sort) {
                ActivitySort.LATEST_FIRST -> COMPARATOR
                ActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
                ActivitySort.SYNC_LATEST_FIRST -> SYNC_COMPARATOR
                ActivitySort.SYNC_EARLIEST_FIRST -> SYNC_COMPARATOR.reversed()
            }
    }
}
