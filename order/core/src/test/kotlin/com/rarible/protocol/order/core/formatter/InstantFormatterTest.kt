package com.rarible.protocol.order.core.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.util.*

internal class InstantFormatterTest {

    private val formatter = InstantFormatter()

    @Test
    fun `parse asctime date`() {
        val instant = formatter.parse("Tue Jun 29 10:00:00 GMT 2021", Locale.ENGLISH)!!
        val date = instant.atOffset(ZoneOffset.UTC)
        assertEquals(29, date.dayOfMonth)
        assertEquals(6, date.monthValue)
        assertEquals(10, date.hour)
    }

    @Test
    fun `parse iso date`() {
        val instant = formatter.parse("2021-06-29T10:00:00Z", Locale.ENGLISH)!!
        val date = instant.atOffset(ZoneOffset.UTC)
        assertEquals(29, date.dayOfMonth)
        assertEquals(6, date.monthValue)
        assertEquals(10, date.hour)
    }

    @Test
    fun `parse same date with different formats`() {
        val date1 = formatter.parse("2021-06-29T10:00:00Z", Locale.ENGLISH)!!
        val date2 = formatter.parse("2021-06-29T10:00:00.000Z", Locale.ENGLISH)!!
        val date3 = formatter.parse("2021-06-29T10:00:00.000000Z", Locale.ENGLISH)!!
        val date4 = formatter.parse("2021-06-29T10:00:00.000000000Z", Locale.ENGLISH)!!
        val date5 = formatter.parse("Tue Jun 29 10:00:00 GMT 2021", Locale.ENGLISH)!!

        assertEquals(date1, date2)
        assertEquals(date1, date3)
        assertEquals(date1, date4)
        assertEquals(date1, date5)
    }

}