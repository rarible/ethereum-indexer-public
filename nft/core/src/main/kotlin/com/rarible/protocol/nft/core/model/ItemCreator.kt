package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document("item_creator")
data class ItemCreator(
    @Id
    val id: ItemId,
    val creator: Address
)
