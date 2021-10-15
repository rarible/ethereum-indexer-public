package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ItemIdDto

object ItemIdParser {
    /**
     * Qualifiers like "0xa7ee407497b2aeb43580cabe2b04026b5419d1dc:123" or "0xa7ee407497b2aeb43580cabe2b04026b5419d1dc:0x0000000000000000000000000000000000000031"
     */
    fun parse(value: String): ItemIdDto {
        val parts = IdParser.split(value, 2)
        return ItemIdDto(
            token = AddressParser.parse(parts[0]),
            tokenId = parts[1]
        )
    }
}
