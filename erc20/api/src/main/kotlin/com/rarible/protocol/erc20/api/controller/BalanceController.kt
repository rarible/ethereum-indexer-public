package com.rarible.protocol.erc20.api.controller

import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.dto.EthBalanceDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.erc20.api.service.balance.Erc20BalanceApiService
import com.rarible.protocol.erc20.api.service.balance.EthBalanceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BalanceController(
    private val erc20BalanceApiService: Erc20BalanceApiService,
    private val ethBalanceService: EthBalanceService
) : BalanceControllerApi {

    override suspend fun getEthBalance(owner: String): ResponseEntity<EthBalanceDto> {
        val ownerAddress = AddressParser.parse(owner)
        return ResponseEntity.ok(ethBalanceService.getBalance(ownerAddress))
    }

    override suspend fun getErc20Balance(contract: String, owner: String): ResponseEntity<Erc20DecimalBalanceDto> {
        val balanceDto = erc20BalanceApiService.get(
            contract = AddressParser.parse(contract),
            owner = AddressParser.parse(owner)
        )
        return ResponseEntity.ok(balanceDto)
    }

}
