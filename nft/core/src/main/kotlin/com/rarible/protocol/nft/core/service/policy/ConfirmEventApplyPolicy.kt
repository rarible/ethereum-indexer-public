package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class ConfirmEventApplyPolicy<T : BlockchainEntityEvent<T>>(
    private val confirmationBlocks: Int
) : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        val newEventList = (events + event)
        val lastNotRevertableEvent = newEventList.lastOrNull { current ->
            current.isConfirmed && isNotReverted(incomeEvent = event, current = current)
        }
        return newEventList.filter { current ->
            current.isConfirmed.not() || current == lastNotRevertableEvent || isReverted(incomeEvent = event, current = current)
        }
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        val lastAppliedEvent = events.lastOrNull { it.isConfirmed }
        return !(lastAppliedEvent == null || lastAppliedEvent < event)
    }

    private fun isReverted(incomeEvent: T, current: T): Boolean {
        return isNotReverted(incomeEvent, current).not()
    }

    private fun isNotReverted(incomeEvent: T, current: T): Boolean {
        val incomeBlockNumber = requireNotNull(incomeEvent.blockNumber)
        val currentBlockNumber = requireNotNull(current.blockNumber)
        val blockDiff = incomeBlockNumber - currentBlockNumber

        require(blockDiff >= 0) {
            "Block diff between income=$incomeEvent and current=$current can't be negative"
        }
        return blockDiff >= confirmationBlocks
    }
}

