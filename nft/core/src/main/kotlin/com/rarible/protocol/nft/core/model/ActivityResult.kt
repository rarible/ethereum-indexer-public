package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.LogEvent
import org.bson.types.ObjectId
import java.time.Instant

data class ActivityResult(val value: LogEvent) {
    fun getId(): ObjectId = this.value.id

    fun getDate(): Instant = (this.value.data as ItemHistory).date

    companion object {
        fun comparator(): Comparator<ActivityResult> = compareByDescending(ActivityResult::getDate)
            .then(compareByDescending(ActivityResult::getId))
    }
}
