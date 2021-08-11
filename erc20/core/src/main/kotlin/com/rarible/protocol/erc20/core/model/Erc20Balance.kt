package com.rarible.protocol.erc20.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document(collection = "erc20_balance")
data class Erc20Balance(
    val token: Address,
    val owner: Address,
    val balance: EthUInt256,
    @Version
    val version: Long? = null
) : EventData {

    @Transient
    private val _id: BalanceId = BalanceId(token, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: BalanceId
        get() = _id
        set(_) {}

    fun withBalance(balance: EthUInt256): Erc20Balance {
        return copy(balance = balance)
    }
}