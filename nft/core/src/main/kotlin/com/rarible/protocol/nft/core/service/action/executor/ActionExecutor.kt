package com.rarible.protocol.nft.core.service.action.executor

import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ActionType

interface ActionExecutor<in T : Action> {
    val type: ActionType

    suspend fun execute(action: T)
}
