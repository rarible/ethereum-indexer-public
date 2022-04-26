package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import scalether.domain.Address

data class InconsistentItem(
    val token: Address,
    val tokenId: EthUInt256,
    val supply: EthUInt256,
    val ownerships: EthUInt256,
    val supplyValue: Long,
    val ownershipsValue: Long,
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