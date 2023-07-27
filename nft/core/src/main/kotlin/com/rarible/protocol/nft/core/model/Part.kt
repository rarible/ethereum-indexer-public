package com.rarible.protocol.nft.core.model

import org.springframework.data.mongodb.core.mapping.Field
import scalether.domain.Address

data class Part(
    @Field(name = "recipient")
    val account: Address,
    val value: Int
) {
    companion object {
        fun fullPart(account: Address): Part {
            return Part(account, 10000)
        }
    }
}
