package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import java.time.Instant

interface AggregatorFetchState {
    val id: String
    val cursor: String

}

data class SeaportFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    fun withCursor(cursor: String): SeaportFetchState {
        return copy(cursor = cursor)
    }
    companion object {
        const val ID = "seaport-order-fetch"
    }
}

data class LooksrareFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {

    @get:Transient
    val listedAfter: Instant
        get() = Instant.ofEpochSecond(cursor.toLong())

    fun withListedAfter(listedAfter: Instant): LooksrareFetchState {
        return copy(cursor = listedAfter.epochSecond.toString())
    }

    companion object {
        fun withListedAfter(listedAfter: Instant): LooksrareFetchState {
            return LooksrareFetchState(cursor = listedAfter.epochSecond.toString())
        }

        const val ID = "looksrare-order-fetch"
    }
}

data class X2Y2FetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    fun withCursor(cursor: String): X2Y2FetchState {
        return copy(cursor = cursor)
    }
    companion object {
        const val ID = "x2y2-order-fetch"
    }
}