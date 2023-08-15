package com.rarible.protocol.nft.core.service.action

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.nft.core.model.ActionEvent
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.model.BurnItemAction
import com.rarible.protocol.nft.core.model.BurnItemActionEvent
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(SpanType.APP)
class ActionEventHandler(
    private val nftItemActionEventRepository: NftItemActionEventRepository,
    private val incomeBurnActionMetric: RegisteredCounter,
    private val itemReduceService: ItemReduceService
) : RaribleKafkaEventHandler<ActionEvent> {

    override suspend fun handle(event: ActionEvent) = when (event) {
        is BurnItemActionEvent -> {
            handleBurnActionEvent(event)
        }
    }

    private suspend fun handleBurnActionEvent(event: BurnItemActionEvent) {
        val existedActions = nftItemActionEventRepository.findByItemIdAndType(event.itemId(), ActionType.BURN)
        val lastUpdatedAt = nowMillis()
        val burnItemAction = BurnItemAction(
            token = event.token,
            tokenId = event.tokenId,
            createdAt = lastUpdatedAt,
            lastUpdatedAt = lastUpdatedAt,
            state = ActionState.PENDING,
            actionAt = event.burnAt
        )
        val (burnAction, needSave) = if (existedActions.isNotEmpty()) {
            val existedAction = existedActions.single() as BurnItemAction
            if (event.burnAt > existedAction.actionAt) {
                existedAction.copy(
                    actionAt = event.burnAt,
                    lastUpdatedAt = lastUpdatedAt,
                    state = ActionState.PENDING
                ) to true
            } else {
                existedAction to false
            }
        } else {
            burnItemAction to true
        }
        if (needSave) {
            nftItemActionEventRepository.save(burnAction).awaitFirst()
            itemReduceService.update(burnAction.token, burnAction.tokenId).awaitFirst()
            incomeBurnActionMetric.increment()
            logger.info("Save action for item ${event.itemId().decimalStringValue}: $burnAction")
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ActionEventHandler::class.java)
    }
}
