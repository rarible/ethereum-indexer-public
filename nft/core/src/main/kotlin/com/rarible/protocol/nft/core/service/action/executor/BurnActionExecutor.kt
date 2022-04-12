package com.rarible.protocol.nft.core.service.action.executor

import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.model.BurnItemAction
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BurnActionExecutor(
    private val reducer: ItemReduceService
) : ActionExecutor<BurnItemAction> {
    override val type: ActionType = ActionType.BURN

    override suspend fun execute(action: BurnItemAction) {
        val itemId = reducer.update(token = action.token, tokenId = action.tokenId).awaitFirstOrNull()?.decimalStringValue
        if (itemId != null) {
            loader.info("Action burn for $itemId was executed")
        } else {
            loader.error("Can't execute action for $itemId")
        }
    }

    private companion object {
        val loader: Logger = LoggerFactory.getLogger(BurnActionExecutor::class.java)
    }
}