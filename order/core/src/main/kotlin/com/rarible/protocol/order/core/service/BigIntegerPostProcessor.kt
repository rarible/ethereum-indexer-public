package com.rarible.protocol.order.core.service

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import org.springframework.boot.jackson.JsonComponent
import java.math.BigInteger

@JsonComponent
class BigIntegerPostProcessor {

    object Serializer : StdScalarSerializer<BigInteger>(BigInteger::class.java) {
        override fun serialize(value: BigInteger, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.toString())
        }
    }

    object Deserializer : StdScalarDeserializer
    <BigInteger>(BigInteger::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BigInteger {
            return when (p.currentToken) {
                JsonToken.VALUE_STRING -> BigInteger(p.text.trim())
                JsonToken.VALUE_NUMBER_INT -> BigInteger(p.text.trim())
                else -> ctxt.handleUnexpectedToken(_valueClass, p) as BigInteger
            }
        }
    }
}


