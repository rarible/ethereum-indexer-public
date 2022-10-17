package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.InconsistentItem.Companion.COLLECTION
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Document(COLLECTION)
@CompoundIndexes(
    CompoundIndex(def = "{token: 1, tokenId: 1}", background = true, unique = true, sparse = true)
)
data class InconsistentItem(
    val token: Address,
    val tokenId: EthUInt256,
    val status: InconsistentItemStatus? = InconsistentItemStatus.UNFIXED,
    val type: ItemProblemType = ItemProblemType.SUPPLY_MISMATCH,
    val fixVersionApplied: Int? = 1,
    val lastUpdatedAt: Instant?,
    val supply: EthUInt256?,
    val ownerships: EthUInt256?,
    val supplyValue: BigInteger?,
    val ownershipsValue: BigInteger?,
) {
    @Transient
    private val _id: ItemId = ItemId(token, tokenId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: ItemId
        get() = _id
        set(_) {}

    companion object {
        const val COLLECTION = "inconsistent_items"
    }
}

enum class InconsistentItemStatus {
    NEW,
    FIXED,
    UNFIXED,
    RELAPSED,
}
