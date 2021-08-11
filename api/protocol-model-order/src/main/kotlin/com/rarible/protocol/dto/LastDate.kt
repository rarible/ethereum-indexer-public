package com.rarible.protocol.dto

import io.daonomic.rpc.domain.Word
import java.math.BigDecimal
import java.time.Instant

sealed class Continuation {
    abstract val afterId: Word

    data class LastDate(
        val afterDate: Instant,
        override val afterId: Word
    ) : Continuation() {
        override fun toString(): String {
            return "${afterDate.toEpochMilli()}_${afterId.hex()}"
        }
    }

    data class Price(val afterPrice: BigDecimal, override val afterId: Word) : Continuation() {
        override fun toString(): String {
            return "${afterPrice}_${afterId.hex()}"
        }

    }

    companion object {
        inline fun <reified T : Continuation> parse(str: String?): T? {
            return if (str == null || !str.contains('_')) {
                null
            } else {
                val (sortField, idStr) = str.split('_')

                when (T::class) {
                    LastDate::class -> LastDate(Instant.ofEpochMilli(sortField.toLong()), Word.apply(idStr))
                    Price::class -> Price(BigDecimal(sortField), Word.apply(idStr))
                    else -> null
                } as T?
            }
        }
    }

}
