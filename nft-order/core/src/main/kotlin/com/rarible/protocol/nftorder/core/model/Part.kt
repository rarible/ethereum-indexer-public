package com.rarible.protocol.nftorder.core.model

import org.springframework.data.mongodb.core.mapping.Field
import scalether.domain.Address

data class Part(
    @Field(name = "recipient")
    val account: Address,
    val value: Int
)