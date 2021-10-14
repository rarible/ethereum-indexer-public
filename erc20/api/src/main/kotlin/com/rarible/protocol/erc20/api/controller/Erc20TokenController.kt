package com.rarible.protocol.erc20.api.controller

import com.rarible.protocol.dto.Erc20TokenDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.erc20.api.service.token.Erc20Service
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class Erc20TokenController(
    private val erc20Service: Erc20Service
) : Erc20TokenControllerApi {

    override suspend fun getErc20TokenById(contract: String): ResponseEntity<Erc20TokenDto> {
        val tokenDto = erc20Service.get(AddressParser.parse(contract))
        return ResponseEntity.ok(tokenDto)
    }
}
