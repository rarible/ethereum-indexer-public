package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderSeaportDataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OpenSeaSigner
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.seaportError
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OpenSeaOrderValidatorImp(
    private val openSeaSigner: OpenSeaSigner,
    private val commonSigner: CommonSigner,
    private val callDataEncoder: CallDataEncoder,
    private val metrics: ForeignOrderMetrics,
    private val seaportLoadProperties: SeaportLoadProperties,
    private val properties: OrderIndexerProperties
) : OpenSeaOrderValidator {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val chainId = BigInteger.valueOf(properties.chainId.toLong())

    override fun validate(order: OrderVersion): Boolean {
        return when (order.type) {
            OrderType.OPEN_SEA_V1 -> innerOpenSeaValidate(order)
            OrderType.SEAPORT_V1 -> innerSeaportValidate(order)
            else -> false
        }
    }

    private fun innerSeaportValidate(order: OrderVersion): Boolean {
        if (seaportLoadProperties.validateSignature.not()) {
            return true
        }
        val data = order.data as? OrderSeaportDataV1 ?: run {
            logger.seaportError("Invalid order (data): $order")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_order_data")
            return false
        }
        val signature = order.signature ?: run {
            logger.seaportError("Invalid order (empty signature): $order")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "empty_signature")
            return false
        }
        val domain = EIP712Domain(
            name = "Seaport",
            version = "1.1",
            chainId = chainId,
            verifyingContract = data.protocol
        )

        if (order.makePrice != null && order.makePrice!! <= properties.minSeaportMakePrice) {
            logger.info("Invalid OpenSea order makePrice (${properties.minSeaportMakePrice}): ${order.makePrice}")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_make_price")
            return false
        }
        return try {
            val hashToSign = domain.hashToSign(order.hash)
            val result = commonSigner.recover(hashToSign, signature) == order.maker
            if (result.not()) {
                logger.seaportError("Invalid order signature, maker doesn't match: $order")
                metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_signature")
            }
            result
        } catch (ex: Exception) {
            logger.seaportError("Invalid order (exception signature): $order", ex)
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_signature")
            false
        }
    }

    private fun innerOpenSeaValidate(order: OrderVersion): Boolean {
        val data = order.data as? OrderOpenSeaV1DataV1 ?: run {
            logger.info("Invalid OpenSea order (data): $order")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_order_data")
            return false
        }
        try {
            callDataEncoder.decodeTransfer(data.callData)
        } catch (ex: Throwable) {
            logger.info("Invalid OpenSea order (calldata): $order")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_call_data")
            return false
        }
        val signature = order.signature ?: run {
            logger.info("Invalid OpenSea order (empty signature): $order")
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "empty_signature")
            return false
        }
        val hashToSign: Word =
            if (data.nonce != null) order.hash else openSeaSigner.openSeaHashToSign(order.hash, false)
        try {
            if (commonSigner.recover(hashToSign, signature) != order.maker) {
                logger.info("Invalid OpenSea order (signature): $order")
                metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_signature")
                return false
            }
        } catch (ex: Throwable) {
            logger.error("Invalid OpenSea order (exception signature): $order", ex)
            metrics.onDownloadedOrderError(Platform.OPEN_SEA, "invalid_signature")
            return false
        }
        return true
    }
}
