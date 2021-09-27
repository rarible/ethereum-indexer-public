package com.rarible.protocol.nftorder.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document("ownership")
data class Ownership(

    val contract: Address,
    val tokenId: EthUInt256,
    val creators: List<Part>,
    val owner: Address,
    val value: EthUInt256,
    val lazyValue: EthUInt256 = EthUInt256.ZERO,
    val date: Instant,
    val pending: List<ItemTransfer>,
    val bestSellOrder: ShortOrder?
) {

    @Transient
    private val _id: OwnershipId = OwnershipId(contract, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: OwnershipId
        get() = _id
        set(_) {}

}