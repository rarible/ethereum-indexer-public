package com.rarible.protocol.erc20.core.model

import com.rarible.core.entity.reducer.model.Entity
import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant

@Document(collection = "erc20_balance")
data class Erc20Balance(
    val token: Address,
    val owner: Address,
    val balance: EthUInt256,

    // TODO these fields should be required later when entire collection will be updated
    val createdAt: Instant?,
    val lastUpdatedAt: Instant?,

    // This is the block which causes the balance change
    val blockNumber: Long?,

    @Version
    override val version: Long? = null,

    override val revertableEvents: List<Erc20Event> = emptyList(),
) : Entity<BalanceId, Erc20Event, Erc20Balance> {

    @Transient
    private val _id: BalanceId = BalanceId(token, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: BalanceId
        get() = _id
        set(_) {}

    override fun withRevertableEvents(events: List<Erc20Event>): Erc20Balance {
        return copy(revertableEvents = events)
    }

    fun withBalance(balance: EthUInt256): Erc20Balance {
        return copy(balance = balance)
    }
}
