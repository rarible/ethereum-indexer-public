package com.rarible.protocol.order.core.model

import com.rarible.core.common.ifNotBlank
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import java.time.Instant

interface AggregatorFetchState {
    val id: String
    val cursor: String

    fun withCursor(cursor: String): AggregatorFetchState
}

data class SeaportFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    override fun withCursor(cursor: String): SeaportFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "seaport-order-fetch"
    }
}

@Deprecated("Need use LooksrareV2FetchState")
data class LooksrareFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {

    @get:Transient
    val listedAfter: Instant
        get() = Instant.ofEpochSecond(cursor.toLong())

    fun withListedAfter(listedAfter: Instant): LooksrareFetchState {
        return withCursor(listedAfter.epochSecond.toString())
    }

    override fun withCursor(cursor: String): LooksrareFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        fun withListedAfter(listedAfter: Instant): LooksrareFetchState {
            return LooksrareFetchState(cursor = listedAfter.epochSecond.toString())
        }

        const val ID = "looksrare-order-fetch"
    }
}

interface LooksrareV2State : AggregatorFetchState {
    val cursorObj: LooksrareV2Cursor?
    val looksrareV2Cursor: LooksrareV2Cursor
    fun withCursor(cursor: LooksrareV2Cursor): LooksrareV2State
}

data class LooksrareV2FetchState(
    override val cursor: String = "",
    @Id
    override val id: String = ID,
    override val cursorObj: LooksrareV2Cursor? = null,
) : LooksrareV2State {

    @get:Transient
    override val looksrareV2Cursor: LooksrareV2Cursor
        get() = cursorObj ?: cursor.ifNotBlank()?.let { LooksrareV2Cursor.parser(it) } ?: LooksrareV2Cursor.default()

    override fun withCursor(cursor: LooksrareV2Cursor): LooksrareV2FetchState {
        return copy(cursorObj = cursor, cursor = "")
    }

    override fun withCursor(cursor: String): LooksrareV2FetchState {
        throw UnsupportedOperationException("Use withCursor(LooksrareV2Cursor)")
    }

    companion object {
        const val ID = "looksrare-v2-order-fetch"
    }
}

data class LooksrareV2CancelListEventFetchState(
    override val cursor: String = "",
    @Id
    override val id: String = ID,
    override val cursorObj: LooksrareV2Cursor? = null,
) : LooksrareV2State {

    @get:Transient
    override val looksrareV2Cursor: LooksrareV2Cursor
        get() = cursorObj ?: cursor.ifNotBlank()?.let { LooksrareV2Cursor.parser(it) } ?: LooksrareV2Cursor.default()

    override fun withCursor(cursor: LooksrareV2Cursor): LooksrareV2CancelListEventFetchState {
        return copy(cursorObj = cursor, cursor = "")
    }

    override fun withCursor(cursor: String): LooksrareV2FetchState {
        throw UnsupportedOperationException("Use withCursor(LooksrareV2Cursor)")
    }

    companion object {
        const val ID = "looksrare-v2-cancel-list-event-fetch"
    }
}

data class X2Y2FetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    override fun withCursor(cursor: String): X2Y2FetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "x2y2-order-fetch"
    }
}

data class X2Y2CancelListEventFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    override fun withCursor(cursor: String): X2Y2CancelListEventFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "x2y2-cancel-list-event-fetch"
    }
}

data class ReservoirAsksEventFetchState(
    override val cursor: String,
    @Id
    override val id: String = ID
) : AggregatorFetchState {
    override fun withCursor(cursor: String): ReservoirAsksEventFetchState {
        return copy(cursor = cursor)
    }

    companion object {
        const val ID = "reservoir-asks-event-fetch"
    }
}
