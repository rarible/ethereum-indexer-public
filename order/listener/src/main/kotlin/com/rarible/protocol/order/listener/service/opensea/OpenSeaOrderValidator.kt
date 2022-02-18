package com.rarible.protocol.order.listener.service.opensea

import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenSeaOrderValidator(
    private val commonSigner: CommonSigner,
    private val callDataEncoder: CallDataEncoder
) {
    fun validate(order: OrderVersion): Boolean {
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
        val hashToSign = commonSigner.openSeaHashToSign(order.hash)
        if (commonSigner.recover(hashToSign, signature) != order.maker) {
            logger.info("Invalid OpenSea order (signature): $order")
            return false
        }
        return true
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(OpenSeaOrderValidator::class.java)
    }
}
