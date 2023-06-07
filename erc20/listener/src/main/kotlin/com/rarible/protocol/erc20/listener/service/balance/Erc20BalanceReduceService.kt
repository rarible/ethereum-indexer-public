package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.repository.Erc20TransferHistoryRepository
import com.rarible.protocol.erc20.core.service.reduce.Erc20BalanceFullReduceService
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventConverter
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import scalether.domain.Address

@Service
class Erc20BalanceReduceService(
    private val erc20BalanceFullReduceService: Erc20BalanceFullReduceService,
    private val erc20TransferHistoryRepository: Erc20TransferHistoryRepository,
    private val erc20EventConverter: Erc20EventConverter,
    ignoredOwnersResolver: IgnoredOwnersResolver
) {

    private val ignoredOwners = ignoredOwnersResolver.resolve()

    fun update(token: Address?, owner: Address?, from: BalanceId?): Flux<Erc20Balance> {
        val events = findOwnerLogEvents(token, owner, from = from)
            .asFlow()
            .filter { it.event.owner !in ignoredOwners }
        return erc20BalanceFullReduceService.reduce(events).asFlux()
    }

    suspend fun update(token: Address, owner: Address): Erc20Balance? {
        return update(token, owner, from = null).collectList().awaitFirstOrNull()?.firstOrNull()
    }

    private fun findOwnerLogEvents(token: Address?, owner: Address?, from: BalanceId?): Flux<Erc20MarkedEvent> {
        return erc20TransferHistoryRepository.findOwnerLogEvents(token = token, owner = owner, from = from)
            .mapNotNull { erc20EventConverter.convert(it.log) }
    }
}
