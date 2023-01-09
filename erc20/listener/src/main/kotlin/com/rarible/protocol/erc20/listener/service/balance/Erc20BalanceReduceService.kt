package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.ReduceService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.service.reduce.Erc20BalanceFullReduceService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import scalether.domain.Address

typealias Erc20BalanceReduceServiceV1 = ReduceService<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId>

@Service
class Erc20BalanceReduceService(
    private val erc20BalanceReduceEventRepository: Erc20BalanceReduceEventRepository,
    private val erc20BalanceFullReduceService: Erc20BalanceFullReduceService
) {
    fun update(token: Address?, owner: Address?, from: BalanceId?): Flux<BalanceId> {
        val events = erc20BalanceReduceEventRepository.findOwnerLogEvents(token, owner, from = from)
        return erc20BalanceFullReduceService.reduce(events.asFlow())
            .map { it.id }
            .asFlux()
    }
}
