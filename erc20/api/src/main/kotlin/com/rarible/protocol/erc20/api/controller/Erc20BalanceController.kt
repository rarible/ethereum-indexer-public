package com.rarible.protocol.erc20.api.controller

import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.erc20.api.service.balance.Erc20BalanceApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@RestController
class Erc20BalanceController(
    private val erc20BalanceApiService: Erc20BalanceApiService
) : Erc20BalanceControllerApi {

    override suspend fun getErc20Balance(contract: String, owner: String): ResponseEntity<Erc20DecimalBalanceDto> =
        ResponseEntity.ok(erc20BalanceApiService.get(Address.apply(contract), Address.apply(owner)))
}
