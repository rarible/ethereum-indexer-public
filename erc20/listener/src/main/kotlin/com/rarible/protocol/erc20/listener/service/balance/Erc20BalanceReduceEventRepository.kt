package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import scalether.domain.Address

@Component
class Erc20BalanceReduceEventRepository(
    private val erc20TransferHistoryRepository: Erc20TransferHistoryRepository,
    private val erc20EventConverter: Erc20EventConverter,
) : ReduceEventRepository<Erc20ReduceEvent, Long, BalanceId> {

    override fun getEvents(key: BalanceId?, after: Long?): Flow<Erc20ReduceEvent> {
        return erc20TransferHistoryRepository.findBalanceLogEvents(key, after)
            .filter { it.blockNumber != null }
            .map { Erc20ReduceEvent(it, it.blockNumber ?: error("BlockNumber can't be null in event ${it.id}")) }
            .asFlow()
    }

    fun findOwnerLogEvents(token: Address?, owner: Address?, from: BalanceId?): Flux<Erc20MarkedEvent> {
        return erc20TransferHistoryRepository.findOwnerLogEvents(token = token, owner = owner, from = from)
            .mapNotNull { erc20EventConverter.convert(it.log) }
    }
}

