package com.rarible.protocol.erc20.core.service

import com.rarible.contracts.erc20.IERC20
import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.event.Erc20BalanceEventListener
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
class Erc20BalanceService(
    private val sender: MonoTransactionSender,
    private val erc20BalanceRepository: Erc20BalanceRepository,
    private val erc20BalanceEventListeners: List<Erc20BalanceEventListener>
) : EntityService<BalanceId, Erc20Balance, Erc20MarkedEvent> {

    override suspend fun update(entity: Erc20Balance, event: Erc20MarkedEvent?): Erc20Balance {
        val result = erc20BalanceRepository.save(entity)
        erc20BalanceEventListeners.forEach {
            it.onUpdate(Erc20UpdateEvent(event?.event, event?.eventTimeMarks, entity))
        }
        return result
    }

    suspend fun get(contract: Address, owner: Address): Erc20Balance? {
        return erc20BalanceRepository.get(BalanceId(contract, owner))
    }

    override suspend fun get(id: BalanceId): Erc20Balance? {
        return erc20BalanceRepository.get(id)
    }

    suspend fun getBlockchainBalance(id: BalanceId): EthUInt256? {
        return IERC20(id.token, sender).balanceOf(id.owner).awaitFirstOrNull()?.let { EthUInt256(it) }
    }
}
