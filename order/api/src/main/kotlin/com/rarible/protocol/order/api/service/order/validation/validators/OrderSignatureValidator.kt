package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.core.validator.OrderValidator
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.CommonSigner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class OrderSignatureValidator(
    @Qualifier("raribleExchangeV2") var eip712Domain: EIP712Domain,
    private val legacySigner: CommonSigner,
    private val erc1271SignService: ERC1271SignService
) : OrderValidator {

    override val type: String = "signature"

    override fun supportsValidation(order: Order): Boolean =
        order.type == OrderType.RARIBLE_V2 || order.type == OrderType.RARIBLE_V1

    override suspend fun validate(order: Order) {
        val signature = order.signature ?: throw OrderUpdateException(
            "Signature is not specified", EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
        )

        return when (order.type) {
            OrderType.RARIBLE_V1 -> {
                logger.info("validating legacy order message: ${order.hash}, signature: $signature")
                val legacyMessage = order.legacyMessage()
                val signer = legacySigner.recover(legacyMessage, signature)
                if (order.maker != signer) {
                    throw OrderUpdateException(
                        "Maker's signature is not valid for V1 order",
                        EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
                    )
                }
                Unit
            }
            OrderType.RARIBLE_V2 -> {
                logger.info("validating v2 order message: ${order.hash}, signature: $signature, eip712Domain: $eip712Domain")
                val structHash = Order.hash(order)
                val hash = eip712Domain.hashToSign(structHash)
                if (erc1271SignService.isSigner(order.maker, hash, signature).not()) {
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
