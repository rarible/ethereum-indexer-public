package com.rarible.protocol.gateway.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
object StringToAddressConverter : Converter<String?, Address> {
    override fun convert(source: String?): Address {
        return Address.apply(source)
    }
}