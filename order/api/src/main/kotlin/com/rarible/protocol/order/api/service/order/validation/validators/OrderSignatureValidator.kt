package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
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
) : OrderFormValidator {

    override suspend fun validate(form: OrderForm) {
        val signature = form.signature

        return when (form.type) {
            OrderType.RARIBLE_V1 -> {
                with(form) {
                    logger.info("validating legacy order message: $hash, signature: $signature")
                    val legacyMessage = legacyMessage(maker, make, take, salt, data)
                    val signer = legacySigner.recover(legacyMessage, signature)
                    if (form.maker != signer) {
                        throw OrderUpdateException(
                            "Maker's signature is not valid for V1 order",
                            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
                        )
                    }
                }
            }
            OrderType.RARIBLE_V2 -> {
                with(form) {
                    logger.info("validating v2 order message: $hash, signature: $signature, eip712Domain: $eip712Domain")
                    val structHash = Order.hash(maker, make, taker, take, salt, start, end, data, type)
                    val hash = eip712Domain.hashToSign(structHash)
                    if (erc1271SignService.isSigner(maker, hash, signature).not()) {
                        throw OrderUpdateException(
                            "Maker's signature is not valid for V2 order",
                            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE
                        )
                    }
                }
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
