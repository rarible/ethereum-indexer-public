package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderSignatureController(
    private val erc1271SignService: ERC1271SignService
) : OrderSignatureControllerApi {

    override suspend fun validate(form: EthereumSignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = erc1271SignService.isSigner(form.signer, form.message, form.signature)
        return ResponseEntity.ok(result)
    }
}