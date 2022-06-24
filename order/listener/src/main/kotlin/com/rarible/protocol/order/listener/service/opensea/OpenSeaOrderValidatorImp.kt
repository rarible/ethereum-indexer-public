package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OpenSeaSigner
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OpenSeaOrderValidatorImp(
    private val openSeaSigner: OpenSeaSigner,
    private val commonSigner: CommonSigner,
    private val callDataEncoder: CallDataEncoder,
    private val openSeaErrorCounter: RegisteredCounter,
    private val seaportErrorCounter: RegisteredCounter,
    properties: OrderIndexerProperties
) : OpenSeaOrderValidator {

    private val chainId = BigInteger.valueOf(properties.chainId.toLong())

    override fun validate(order: OrderVersion): Boolean {
        return when (order.type) {
            OrderType.OPEN_SEA_V1 -> {
                val result = innerOpenSeaValidate(order)
                if (result.not()) openSeaErrorCounter.increment()
                result
            }
            OrderType.SEAPORT_V1 -> {
                val result = innerSeaportValidate(order)
                if (result.not()) seaportErrorCounter.increment()
                result
            }
            OrderType.RARIBLE_V1, OrderType.RARIBLE_V2, OrderType.CRYPTO_PUNKS -> false
        }
    }

    private fun innerSeaportValidate(order: OrderVersion): Boolean {
        val data = order.data as? OrderSeaportDataV1 ?: run {
            logger.seaportError("Invalid order (data): $order")
            return false
        }
        val signature = order.signature ?: run {
            logger.seaportError("Invalid order (empty signature): $order")
            return false
        }
        val domain = EIP712Domain(
            name = "Seaport",
            version = "1.1",
            chainId = chainId,
            verifyingContract = data.protocol
        )
        return try {
            val hashToSign = domain.hashToSign(order.hash)
            val result = commonSigner.recover(hashToSign, signature) == order.maker
            if (result.not()) {
                logger.seaportError("Invalid order signature, maker doesn't match: $order")
            }
            result
        } catch (ex: Exception) {
            logger.seaportError("Invalid order (exception signature): $order", ex)
            false
        }
    }

    private fun innerOpenSeaValidate(order: OrderVersion): Boolean {
        val data = order.data as? OrderOpenSeaV1DataV1 ?: run {
            logger.info("Invalid OpenSea order (data): $order")
            return false
        }
        try {
            callDataEncoder.decodeTransfer(data.callData)
        } catch (ex: Throwable) {
            logger.info("Invalid OpenSea order (calldata): $order")
            return false
        }
        val signature = order.signature ?: run {
            logger.info("Invalid OpenSea order (empty signature): $order")
            return false
        }
        val hashToSign: Word = if (data.nonce != null) order.hash else openSeaSigner.openSeaHashToSign(order.hash, false)
        try {
            if (commonSigner.recover(hashToSign, signature) != order.maker) {
                logger.info("Invalid OpenSea order (signature): $order")
                return false
            }
        } catch (ex: Throwable) {
            logger.error("Invalid OpenSea order (exception signature): $order", ex)
            return false
        }
        return true
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(OpenSeaOrderValidatorImp::class.java)
    }
}
