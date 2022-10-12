package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import scalether.domain.Address
import java.time.Instant


data class InconsistentItem(
    val token: Address,
    val tokenId: EthUInt256,
    val status: InconsistentItemStatus?,
    val type: ItemProblemType = ItemProblemType.SUPPLY_MISMATCH,
    val fixVersionApplied: Int? = 1,
    val lastUpdatedAt: Instant?,
    val supply: EthUInt256?,
    val ownerships: EthUInt256?,
    val supplyValue: Long?,
    val ownershipsValue: Long?,
) {
    @Transient
    private val _id: ItemId = ItemId(token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = _id
        set(_) {}
}

enum class InconsistentItemStatus {
    NEW,
    FIXED,
    UNFIXED,
    RELAPSED,
}
