package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.repository.history.ActivitySort
import org.bson.types.ObjectId
import java.time.Instant

data class ActivityResult(val value: LogEvent) {
    fun getId(): ObjectId = this.value.id

    fun getDate(): Instant = (this.value.data as ItemHistory).date

    companion object {
        private val COMPARATOR =
            compareByDescending(ActivityResult::getDate)
                .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: ActivitySort): Comparator<ActivityResult> =
            when (sort) {
                ActivitySort.LATEST_FIRST -> COMPARATOR
                ActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
            }
    }
}
