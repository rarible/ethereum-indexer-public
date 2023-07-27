package com.rarible.protocol.nft.core.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import scalether.domain.Address
import java.time.Instant

enum class ActionType {
    BURN
}

enum class ActionState {
    PENDING,
    EXECUTED
}

sealed class Action(var type: ActionType) {
    abstract val token: Address
    abstract val tokenId: EthUInt256
    abstract val createdAt: Instant
    abstract val lastUpdatedAt: Instant
    abstract val state: ActionState
    abstract val actionAt: Instant
    abstract val id: String
    abstract val version: Long?

    fun itemId(): ItemId {
        return ItemId(token, tokenId)
    }

    fun isActionable(): Boolean {
        return nowMillis() >= actionAt
    }

    abstract fun withState(state: ActionState): Action
}

data class BurnItemAction(
    override val token: Address,
    override val tokenId: EthUInt256,
    override val createdAt: Instant,
    override val lastUpdatedAt: Instant,
    override val state: ActionState,
    override val actionAt: Instant,
    @Id
    override val id: String = ObjectId().toHexString(),
    @Version
    override val version: Long? = null,
) : Action(ActionType.BURN) {

    override fun withState(state: ActionState): BurnItemAction {
        return copy(state = state)
    }
}
