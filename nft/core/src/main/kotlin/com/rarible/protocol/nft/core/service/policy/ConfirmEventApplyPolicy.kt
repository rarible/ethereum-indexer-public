package com.rarible.protocol.nft.core.service.policy

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.protocol.nft.core.model.EthereumEntityEvent

open class ConfirmEventApplyPolicy<T : EthereumEntityEvent<T>>(
    private val confirmationBlocks: Int
) : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        val newEventList = (events + event)
        val lastNotRevertableEvent = newEventList.lastOrNull { current ->
            current.log.status == EthereumLogStatus.CONFIRMED && isNotReverted(incomeEvent = event, current = current)
        }
        return newEventList
            .filter { current ->
                // we remove all CONFIRMED logs which can't be reverted anymore,
                // except the latest not revertable logs
                // we always must have at least one not revertable log in the list
                current.log.status != EthereumLogStatus.CONFIRMED ||
                        current == lastNotRevertableEvent ||
                        isReverted(incomeEvent = event, current = current)
            }
            .filter { current ->
                // try to remove PENDING logs related to this income CONFIRMED event
                current.log.status == EthereumLogStatus.CONFIRMED ||
                        isNotRelatedPendingLog(incomeEvent = event, pending = current)
            }
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        val lastAppliedEvent = events.lastOrNull { it.log.status == EthereumLogStatus.CONFIRMED }
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

    private fun isNotRelatedPendingLog(incomeEvent: T, pending: T): Boolean {
        return pending.log.status == EthereumLogStatus.PENDING && pending.compareTo(incomeEvent) != 0
    }
}
