package com.rarible.protocol.erc20.api.service.token

import com.rarible.core.common.convert
import com.rarible.core.contract.model.ContractType
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.protocol.dto.Erc20TokenDto
import com.rarible.protocol.erc20.api.exceptions.TokenNotFoundException
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class Erc20Service(
    private val conversionService: ConversionService,
    private val contractService: ContractService
) {
    suspend fun get(token: Address): Erc20TokenDto {
        return contractService
            .get(token)
            .takeIf { it.type == ContractType.ERC20_TOKEN }
            ?.let { conversionService.convert<Erc20TokenDto>(it) }
            ?: throw TokenNotFoundException(token)
    }
}