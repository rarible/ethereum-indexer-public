package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ArgumentFormatException
import java.math.BigInteger

object BigIntegerParser {
    fun parse(bigInteger: String): BigInteger {
        return try {
            BigInteger(bigInteger)
        } catch (ex: Throwable) {
            throw ArgumentFormatException("Can't parser big integer arg '$bigInteger'")
        }
    }
}
