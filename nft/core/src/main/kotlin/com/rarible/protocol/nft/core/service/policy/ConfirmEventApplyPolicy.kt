package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent

open class ConfirmEventApplyPolicy<T : BlockchainEntityEvent<T>>(
    private val confirmationBlocks: Int
) : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        checkIncomeEvent(event)

        val newEventList = (events + event)
        val lastNotRevertableEvent = newEventList.lastOrNull { current ->
            current.isConfirmed && isNotReverted(incomeEvent = event, current = current)
        }
        return newEventList.filter { current ->
            current.isConfirmed && isNotReverted(incomeEvent = event, current = current) && current != lastNotRevertableEvent
        }
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        checkIncomeEvent(event)
        val lastAppliedEvent = events.lastOrNull { it.isConfirmed }

        return if (lastAppliedEvent == null || lastAppliedEvent < event) {
            true
        } else {
            val firstAppliedEvent = events.firstOrNull { it.isConfirmed }
            when  {
                firstAppliedEvent == null -> true
                firstAppliedEvent > lastAppliedEvent -> throw IllegalStateException("Can't decide if need to apply event")
                else -> false
            }
        }
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

    private fun checkIncomeEvent(event: T) {
        require(event.isConfirmed) { "Income event must be with ${BlockchainEntityEvent.Status.CONFIRMED} status" }
    }
}

