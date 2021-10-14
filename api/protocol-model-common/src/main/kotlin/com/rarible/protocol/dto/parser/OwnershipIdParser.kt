package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.OwnershipIdDto

object OwnershipIdParser {
    /**
     * Qualifiers like "0xa7ee407497b2aeb43580cabe2b04026b5419d1dc:123:0xa7ee407497b2aeb43580cabe2b04026b5419d1dc"
     * or "0xa7ee407497b2aeb43580cabe2b04026b5419d1dc:0x0000000000000000000000000000000000000031:0xa7ee407497b2aeb43580cabe2b04026b5419d1dc"
     */
    fun parse(value: String): OwnershipIdDto {
        val parts = IdParser.split(value, 3)
        return OwnershipIdDto(
            token = AddressParser.parse(parts[0]),
            tokenId = parts[1],
            owner = AddressParser.parse(parts[2])
        )
    }
}
