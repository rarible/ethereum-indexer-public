package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.dto.SeaportFulfillmentSimpleResponseDto
import com.rarible.protocol.dto.X2Y2GetCancelInputRequestDto
import com.rarible.protocol.dto.X2Y2OrderSignRequestDto
import com.rarible.protocol.dto.X2Y2SignResponseDto
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.service.SeaportSignatureResolver
import com.rarible.x2y2.client.X2Y2ApiClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderSignatureController(
    private val erc1271SignService: ERC1271SignService,
    private val x2Y2ApiClient: X2Y2ApiClient,
    private val seaportSignatureResolver: SeaportSignatureResolver
) : OrderSignatureControllerApi {

    override suspend fun validate(form: EthereumSignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = erc1271SignService.isSigner(form.signer, form.message, form.signature)

        return ResponseEntity.ok(result)
    }

    override suspend fun cancelSignX2Y2(x2Y2GetCancelInputRequestDto: X2Y2GetCancelInputRequestDto): ResponseEntity<X2Y2SignResponseDto> {
        val response = with(x2Y2GetCancelInputRequestDto) {
            x2Y2ApiClient.getCancelInput(
                caller = caller,
                op = op,
                orderId = orderId,
                signMessage = signMessage,
                sign = sign
            )
        }

        return ResponseEntity.ok(X2Y2SignResponseDto(response.data.input))
    }

    override suspend fun getSeaportOrderSignature(
        hash: String
    ): ResponseEntity<SeaportFulfillmentSimpleResponseDto> {
        val signature = seaportSignatureResolver.resolveSeaportSignature(hash.toOrderId().hash)
        return ResponseEntity.ok(SeaportFulfillmentSimpleResponseDto(signature))
    }

    override suspend fun orderSignX2Y2(x2Y2OrderSignRequestDto: X2Y2OrderSignRequestDto): ResponseEntity<X2Y2SignResponseDto> {
        val response = with(x2Y2OrderSignRequestDto) {
            x2Y2ApiClient.fetchOrderSign(
                caller = caller,
                op = op,
                orderId = orderId,
                currency = currency,
                price = price,
                tokenId = tokenId
            ).ensureSuccess()
        }
        return ResponseEntity.ok(X2Y2SignResponseDto(response.data.single().input))
    }
}
