package com.rarible.protocol.order.core.formatter

import org.springframework.format.Formatter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

class InstantFormatter : Formatter<Instant?> {

    private val requestDateTimeParser = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
        .appendPattern("[.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]")
        .appendOffset("+HH:mm", "Z")
        .toFormatter()
        .withZone(ZoneOffset.UTC)

    private val dateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        .withZone(ZoneOffset.UTC)

    private val legacyDateTimeFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
    }

    @Throws(ParseException::class)
    override fun parse(text: String?, locale: Locale): Instant? {
        if (text == null) {
            return null
        }
        return try {
            OffsetDateTime.parse(text, requestDateTimeParser).toInstant()
        } catch (ex: Throwable) {
            val date = legacyDateTimeFormatter.get().parse(text)
            Instant.ofEpochMilli(date.time)
        }
    }

    override fun print(dateTime: Instant?, locale: Locale): String? {
        return if (dateTime == null) {
            null
        } else {
            dateTimeFormatter.format(dateTime)
        }
    }

}
