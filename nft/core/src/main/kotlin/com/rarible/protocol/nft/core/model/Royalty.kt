package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document(collection = "royalty")
data class Royalty(
    val address: Address,
    val tokenId: EthUInt256,
    val royalty: List<Part>
) {
    @Transient
    private val _id: ItemId = ItemId(address, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = _id
        set(_) {}

    companion object {
        fun empty() = Royalty(
            address = Address.ZERO(),
            tokenId = EthUInt256.ZERO,
            royalty = listOf()
        )
    }
}
