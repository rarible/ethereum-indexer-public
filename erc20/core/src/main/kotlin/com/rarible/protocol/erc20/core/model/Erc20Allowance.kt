package com.rarible.protocol.erc20.core.model

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

@Document(collection = "erc20_allowance")
data class Erc20Allowance(
    val token: Address,
    val owner: Address,
    val allowance: EthUInt256,

    val createdAt: Instant,
    val lastUpdatedAt: Instant,

    @Version
    override val version: Long? = null,
) : Entity<BalanceId, Erc20Event, Erc20Allowance> {
    @Transient
    private val _id: BalanceId = BalanceId(token, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    override var id: BalanceId
        get() = _id
        set(_) {}

    @get:Transient
    override val revertableEvents: List<Erc20Event>
        get() = emptyList()

    override fun withRevertableEvents(events: List<Erc20Event>): Erc20Allowance = this

    fun withAllowance(allowance: EthUInt256): Erc20Allowance =
        copy(allowance = allowance, lastUpdatedAt = nowMillis())
}