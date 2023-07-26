package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.model.Entity
import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document(Ownership.COLLECTION)
data class Ownership(
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
    val lastLazyEventTimestamp: Long? = null,

    // This is the block which causes the balance change
    val blockNumber: Long? = null,

    override val revertableEvents: List<OwnershipEvent> = emptyList(),
    @Version
    override val version: Long? = null
) : Entity<OwnershipId, OwnershipEvent, Ownership> {

    @Transient
    private val _id: OwnershipId = OwnershipId(token, tokenId, owner)

    fun getPendingEvents(): List<OwnershipEvent> {
        return revertableEvents.filter { it.log.status == EthereumBlockStatus.PENDING }
    }

    fun withVersion(version: Long?): Ownership {
        return copy(version = version)
    }

    fun isLazyOwnership(): Boolean {
        return lastLazyEventTimestamp != null
    }

    fun withBlockNumber(blockNumber: Long?): Ownership {
        return copy(blockNumber = blockNumber)
    }

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: OwnershipId
        get() = _id
        set(_) {}

    override fun withRevertableEvents(events: List<OwnershipEvent>): Ownership {
        return copy(revertableEvents = events)
    }

    fun withCalculatedFields(): Ownership {
        val deleted = this.lazyValue == EthUInt256.ZERO &&
            this.value == EthUInt256.ZERO &&
            this.getPendingEvents().isEmpty() &&
            this.pending.isEmpty() // TODO this check not needed for reducerV2
        val updatedAt =
            // We try to get timestamp of the latest blockchain event
            this.revertableEvents.lastOrNull { it.log.status == EthereumBlockStatus.CONFIRMED }?.log?.createdAt
            // If no blockchain event, we get latest pending createdAt event timestamp
            ?: this.revertableEvents.lastOrNull { it.log.status == EthereumBlockStatus.PENDING }?.log?.createdAt ?: this.date
        return this.copy(deleted = deleted, date = updatedAt)
    }

    companion object {
        const val COLLECTION = "ownership"

        fun parseId(id: String): OwnershipId {
            val parts = id.split(":")
            if (parts.size < 3) {
                throw IllegalArgumentException("Incorrect format of ownershipId: $id")
            }
            val tokenId = EthUInt256.of(parts[1].trim())
            return OwnershipId(Address.apply(parts[0].trim()), tokenId, Address.apply(parts[2].trim()))
        }

        fun empty(token: Address, tokenId: EthUInt256, owner: Address, version: Long?): Ownership {
            return Ownership(
                token = token,
                tokenId = tokenId,
                creators = emptyList(),
                owner = owner,
                value = EthUInt256.ZERO,
                lazyValue = EthUInt256.ZERO,
                date = nowMillis(),
                lastUpdatedAt = nowMillis(),
                pending = emptyList(),
                version = version
            )
        }
    }
}
