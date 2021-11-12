package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.UpdateService
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.springframework.stereotype.Component

@Component
class Erc20BalanceUpdateService(
    private val balanceRepository: Erc20BalanceService
) : UpdateService<Erc20Balance> {

    override suspend fun update(data: Erc20Balance) {
        val currentBalance = balanceRepository.get(data.id)

        val balanceToSave = currentBalance?.withBalance(data.balance) ?: data
        balanceRepository.save(balanceToSave)
    }
}
