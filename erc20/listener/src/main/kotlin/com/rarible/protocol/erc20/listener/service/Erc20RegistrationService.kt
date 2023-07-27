package com.rarible.protocol.erc20.listener.service

import com.rarible.core.contract.model.Contract
import com.rarible.core.contract.model.ContractType
import com.rarible.ethereum.contract.service.ContractService
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class Erc20RegistrationService(
    private val contractService: ContractService
) {
    suspend fun tryRegister(address: Address): Contract? {
        val contract = contractService.get(address)
        return contract.takeIf { it.type == ContractType.ERC20_TOKEN }
    }
}
