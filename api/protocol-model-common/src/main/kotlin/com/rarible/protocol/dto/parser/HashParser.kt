package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ArgumentFormatException
import io.daonomic.rpc.domain.Word

object HashParser {
    fun parse(hash: String): Word {
        return try {
            Word.apply(hash)
        } catch (ex: Throwable) {
            throw ArgumentFormatException("Can't parser hash arg '$hash'")
        }
    }
}

