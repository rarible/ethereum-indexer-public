package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
class Erc20BalanceReduceEventRepository(
    private val erc20TransferHistoryRepository: Erc20TransferHistoryRepository
) : ReduceEventRepository<Erc20ReduceEvent, Long, BalanceId> {

    override fun getEvents(key: BalanceId?, after: Long?): Flow<Erc20ReduceEvent> {
        return erc20TransferHistoryRepository.findBalanceLogEvents(key, after)
            .filter { it.blockNumber != null }
            .map { Erc20ReduceEvent(it, it.blockNumber ?: error("BlockNumber can't be null in event ${it.id}")) }
            .asFlow()
    }
}

