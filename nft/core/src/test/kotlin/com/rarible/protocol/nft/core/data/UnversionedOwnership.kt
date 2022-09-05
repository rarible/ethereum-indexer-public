package com.rarible.protocol.nft.core.data

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document("ownership")
data class UnversionedOwnership(
    val token: Address,
    val tokenId: EthUInt256,
    @Deprecated("Should be removed")
    val creators: List<Part> = emptyList(),
    val owner: Address,
    val value: EthUInt256,
    val lazyValue: EthUInt256 = EthUInt256.ZERO,
    val date: Instant,
    val lastUpdatedAt: Instant?,
    @Deprecated("Should use getPendingEvents()")
    val pending: List<ItemTransfer>,
    val deleted: Boolean = false,
    val lastLazyEventTimestamp: Long? = null
) {

    @Transient
    private val _id: OwnershipId = OwnershipId(token, tokenId, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: OwnershipId
        get() = _id
        set(_) {}
}
