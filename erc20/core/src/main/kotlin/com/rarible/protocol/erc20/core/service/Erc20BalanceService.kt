package com.rarible.protocol.erc20.core.service

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.erc20.core.listener.Erc20BalanceEventListener
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class Erc20BalanceService(
    private val erc20BalanceRepository: Erc20BalanceRepository,
    private val erc20BalanceEventListeners: List<Erc20BalanceEventListener>
) : EntityService<BalanceId, Erc20Balance> {

    override suspend fun update(entity: Erc20Balance): Erc20Balance {
        val result = erc20BalanceRepository.save(entity)
        erc20BalanceEventListeners.forEach {
            it.onUpdate(Erc20UpdateEvent(entity))
        }
        return result
    }

    suspend fun get(contract: Address, owner: Address): Erc20Balance? {
        return erc20BalanceRepository.get(BalanceId(contract, owner))
    }

    override suspend fun get(id: BalanceId): Erc20Balance? {
        return erc20BalanceRepository.get(id)
    }
}