package com.rarible.protocol.nft.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document("item")
@CompoundIndexes(
    CompoundIndex(def = "{token: 1, tokenId: 1}", background = true, unique = true, sparse = true)
)
data class Item(
    val token: Address,
    val tokenId: EthUInt256,
    val creators: List<Part> = emptyList(),
    val creatorsFinal: Boolean = false,
    val supply: EthUInt256,
    val lazySupply: EthUInt256 = EthUInt256.ZERO,
    val royalties: List<Part>,
    val owners: List<Address> = emptyList(),
    val date: Instant,
    val pending: List<ItemTransfer> = emptyList(),
    val deleted: Boolean = false
) {
    @Transient
    private val _id: ItemId = ItemId(token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = _id
        set(_) {}

    companion object {

        fun parseId(id: String): ItemId {
            val parts = id.split(":")
            if (parts.size < 2) {
                throw IllegalArgumentException("Incorrect format of itemId: $id")
            }
            val tokenId = EthUInt256.of(parts[1].trim())
            return ItemId(Address.apply(parts[0].trim()), tokenId)
        }

        fun empty(token: Address, tokenId: EthUInt256): Item {
            return Item(
                token = token,
                tokenId = tokenId,
                supply = EthUInt256.ZERO,
                lazySupply = EthUInt256.ZERO,
                royalties = emptyList(),
                date = nowMillis()
            )
        }
    }

    object Fields {
        const val ID = "_id"
        val TOKEN = Item::token.name
        val CREATORS_RECIPIENT = "${Item::creators.name}.recipient"
        val OWNERS = Item::owners.name
        val DATE = Item::date.name
    }
}