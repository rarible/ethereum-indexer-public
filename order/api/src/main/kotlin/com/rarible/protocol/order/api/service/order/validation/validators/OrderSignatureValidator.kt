package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.api.service.order.validation.OrderVersionValidator
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.service.CommonSigner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class OrderSignatureValidator(
    @Qualifier("raribleExchangeV2") var eip712Domain: EIP712Domain,
    private val legacySigner: CommonSigner,
    private val erc1271SignService: ERC1271SignService
) : OrderVersionValidator {

    override suspend fun validate(orderVersion: OrderVersion) {
        val signature = orderVersion.signature ?: throw OrderUpdateException(
            "Signature is not specified", EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
        )

        return when (orderVersion.type) {
            OrderType.RARIBLE_V1 -> {
                logger.info("validating legacy order message: ${orderVersion.hash}, signature: $signature")
                val legacyMessage = orderVersion.legacyMessage()
                val signer = legacySigner.recover(legacyMessage, signature)
                if (orderVersion.maker != signer) {
                    throw OrderUpdateException(
                        "Maker's signature is not valid for V1 order",
                        EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
                    )
                }
                Unit
            }
            OrderType.RARIBLE_V2 -> {
                logger.info("validating v2 order message: ${orderVersion.hash}, signature: $signature, eip712Domain: $eip712Domain")
                val structHash = Order.hash(orderVersion)
                val hash = eip712Domain.hashToSign(structHash)
                if (erc1271SignService.isSigner(orderVersion.maker, hash, signature).not()) {
                    throw OrderUpdateException(
                        "Maker's signature is not valid for V2 order",
                        EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
                    )
                }
                Unit
            }
            OrderType.OPEN_SEA_V1 -> Unit
            OrderType.SEAPORT_V1 -> Unit
            OrderType.CRYPTO_PUNKS -> Unit
            OrderType.X2Y2 -> Unit
            OrderType.LOOKSRARE -> Unit
            OrderType.LOOKSRARE_V2 -> Unit
            OrderType.AMM -> Unit
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OrderSignatureValidator::class.java)
    }
}
