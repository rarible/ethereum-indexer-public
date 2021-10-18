package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ArgumentFormatException
import scalether.domain.Address

object AddressParser {
    fun parse(address: String): Address {
        return try {
            Address.apply(address)
        } catch (ex: Throwable) {
            throw ArgumentFormatException("Can't parser address arg '$address'")
        }
    }
}

fun AddressParser.parse(address: String?): Address? {
    return if (address != null) parse(address) else null
}
