package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ArgumentFormatException

object IdParser {
    private const val DELIMITER = ":"

    fun split(value: String, expectedSize: Int): List<String> {
        val parts = value.split(DELIMITER)
        assertSize(value, parts, expectedSize)
        return parts
    }

    private fun assertSize(
        value: String,
        parts: List<String>,
        expectedSize: Int
    ) {
        if (parts.size != expectedSize) {
            throw ArgumentFormatException(
                "Illegal format for ID: '$value', " +
                        "expected $expectedSize parts in ID, concatenated by '$DELIMITER'"
            )
        }
    }
}
