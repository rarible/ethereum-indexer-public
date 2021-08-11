package com.rarible.protocol.erc20.api.service.balance

import com.rarible.core.contract.model.Erc20Token
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.erc20.core.service.Erc20BalanceService
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

@Component
class Erc20BalanceApiService(
    private val contractService: ContractService,
    private val erc20BalanceService: Erc20BalanceService
) {
    suspend fun get(contract: Address, owner: Address): Erc20DecimalBalanceDto {
        val balance = erc20BalanceService.get(contract, owner)
        if (balance != null) {
            val decimals = getContractDecimals(contract)
            val balanceValue = balance.balance.value
            val decimalBalanceValue = balanceValue.toBigDecimal().divide(BigDecimal.TEN.pow(decimals))
            return Erc20DecimalBalanceDto(
                contract = contract,
                owner = owner,
                balance = balanceValue,
                decimalBalance = decimalBalanceValue
            )
        }
        return Erc20DecimalBalanceDto(contract, owner, BigInteger.ZERO, BigDecimal.ZERO)
    }

    private suspend fun getContractDecimals(contract: Address): Int {
        val entity = contractService.get(contract)
        return if (entity is Erc20Token) {
            entity.decimals ?: 0
        } else {
            0
        }
    }
}
