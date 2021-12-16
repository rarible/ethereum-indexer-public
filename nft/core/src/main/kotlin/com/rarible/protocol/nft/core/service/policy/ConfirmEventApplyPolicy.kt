package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class ConfirmEventApplyPolicy<T : EthereumEntityEvent<T>>(
    private val confirmationBlocks: Int
) : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        val newEventList = (events + event)
        val lastNotRevertableEvent = newEventList.lastOrNull { current ->
            current.log.status == Log.Status.CONFIRMED && isNotReverted(incomeEvent = event, current = current)
        }
        return newEventList.filter { current ->
            current.log.status != Log.Status.CONFIRMED || current == lastNotRevertableEvent || isReverted(
                incomeEvent = event,
                current = current
            )
        }
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        val lastAppliedEvent = events.lastOrNull { it.log.status == Log.Status.CONFIRMED }
        return !(lastAppliedEvent == null || lastAppliedEvent < event)
    }

    private fun isReverted(incomeEvent: T, current: T): Boolean {
        return isNotReverted(incomeEvent, current).not()
    }

    private fun isNotReverted(incomeEvent: T, current: T): Boolean {
        val incomeBlockNumber = requireNotNull(incomeEvent.log.blockNumber)
        val currentBlockNumber = requireNotNull(current.log.blockNumber)
        val blockDiff = incomeBlockNumber - currentBlockNumber

        require(blockDiff >= 0) {
            "Block diff between income=$incomeEvent and current=$current can't be negative"
        }
        return blockDiff >= confirmationBlocks
    }
}
