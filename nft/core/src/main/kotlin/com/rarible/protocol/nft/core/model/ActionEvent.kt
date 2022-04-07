package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.time.Instant

enum class ActionEventType {
    BURN
}

sealed class ActionEvent(val type: ActionType) {
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
) : ActionEvent(ActionType.BURN)