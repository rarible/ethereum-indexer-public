package com.rarible.protocol.nft.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "BURN", value = BurnItemActionEvent::class)
)
sealed class ActionEvent {
    abstract val token: Address
    abstract val tokenId: EthUInt256

    fun itemId(): ItemId {
        return ItemId(token, tokenId)
    }
}

data class BurnItemActionEvent(
    override val token: Address,
    override val tokenId: EthUInt256,
    val burnAt: Instant
) : ActionEvent()
