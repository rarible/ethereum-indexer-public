package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigInteger
import java.util.*

@Document(collection = "royalty")
@CompoundIndexes(
    CompoundIndex(name = "address_tokenId", def = "{'address' : 1, 'tokenId': 1}")
)
data class Royalty(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val address: Address,
    val tokenId: BigInteger,
    val royalty: List<Part>
) {
    companion object {
        fun empty() = Royalty(
            address = Address.ZERO(),
            tokenId = BigInteger.ZERO,
            royalty = listOf()
        )
    }
}
