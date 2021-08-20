package com.rarible.protocol.nftorder.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderDto
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Document("item")
data class Item(

    val token: Address,
    val tokenId: EthUInt256,
    val creators: List<Part> = emptyList(),
    val supply: EthUInt256,
    val lazySupply: EthUInt256 = EthUInt256.ZERO,
    val royalties: List<Part>,
    val owners: List<Address> = listOf(),
    val date: Instant,
    val pending: List<ItemTransfer> = emptyList(),

    val sellers: Int = 0,
    val totalStock: BigInteger,
    val bestSellOrder: OrderDto?,
    val bestBidOrder: OrderDto?,
    val unlockable: Boolean,
    @Version
    val version: Long? = null
) {

    @Transient
    private val _id: ItemId = ItemId(token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = _id
        set(_) {}

}



