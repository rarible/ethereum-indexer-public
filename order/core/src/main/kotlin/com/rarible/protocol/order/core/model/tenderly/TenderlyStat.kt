package com.rarible.protocol.order.core.model.tenderly

import com.rarible.core.common.nowMillis
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document("tenderly_stat")
data class TenderlyStat(
    @Id
    val id: LocalDate,
    val createdAt: Instant = nowMillis(),
    val attempts: Long = 0,
    val requests: Long = 0,
    @Version
    val version: Long? = null
) {

    fun incrementAttempt() = copy(attempts = attempts + 1)
    fun incrementRequest() = copy(requests = requests + 1)

    companion object {
        fun create(date: LocalDate) = TenderlyStat(
            id = date,
        )
    }
}
