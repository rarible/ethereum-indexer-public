package com.rarible.protocol.order.api.converter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.rarible.protocol.order.core.misc.toWord
import io.daonomic.rpc.domain.Word
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class WordDeserializer : StdDeserializer<Word>(Word::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Word? {
        if (p.currentToken().isNumeric) {
            return p.bigIntegerValue.toWord()
        }
        return if (p.valueAsString.startsWith("0x")) {
            Word.apply(p.valueAsString)
        } else {
            p.valueAsString.toBigInteger().toWord()
        }
    }
}
