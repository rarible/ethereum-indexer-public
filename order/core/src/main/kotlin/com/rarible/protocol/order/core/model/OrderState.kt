package com.rarible.protocol.order.core.model

import com.rarible.protocol.dto.OrderStateDto
import io.daonomic.rpc.domain.Word
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("order_state")
data class OrderState(
    @Id
    val id: Word,
    val canceled: Boolean,
    val createdAt: Instant = Instant.now(),
    val lastUpdateAt: Instant = createdAt,
    @Version
    val version: Long? = null
) {
    fun withCanceled(canceled: Boolean): OrderState {
        return copy(canceled = canceled, lastUpdateAt = Instant.now())
    }

    companion object {

        fun default(id: Word) = OrderState(
            id = id,
            canceled = false
        )

        fun toState(hash: Word, dto: OrderStateDto): OrderState {
            return OrderState(id = hash, canceled = dto.canceled)
        }
    }
}
