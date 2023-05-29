package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.ReduceService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.service.reduce.Erc20BalanceFullReduceService
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import scalether.domain.Address

typealias Erc20BalanceReduceServiceV1 = ReduceService<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId>

@Service
class Erc20BalanceReduceService(
    private val erc20BalanceReduceEventRepository: Erc20BalanceReduceEventRepository,
    private val erc20BalanceFullReduceService: Erc20BalanceFullReduceService,
    ignoredOwnersResolver: IgnoredOwnersResolver
) {

    private val ignoredOwners = ignoredOwnersResolver.resolve()

    fun update(token: Address?, owner: Address?, from: BalanceId?): Flux<Erc20Balance> {
        val events = erc20BalanceReduceEventRepository.findOwnerLogEvents(token, owner, from = from)
            .asFlow()
            .filter { it.owner !in ignoredOwners }
        return erc20BalanceFullReduceService.reduce(events).asFlux()
    }

    suspend fun update(token: Address, owner: Address): Erc20Balance? {
        return update(token, owner, from = null).collectList().awaitFirstOrNull()?.firstOrNull()
    }
}
