package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.exchange.blur.v1.NonceIncrementedEvent
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.blur.BlurEventConverter
import com.rarible.protocol.order.listener.service.descriptors.NonceSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
@EnableBlur
class BlurExchangeChangeCounterDescriptor(
    contractsProvider: ContractsProvider,
    private val blurEventConverter: BlurEventConverter,
) : NonceSubscriber(
    name = "blur_nonce_incremented",
    topic = NonceIncrementedEvent.id(),
    contracts = contractsProvider.blurV1()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<ChangeNonceHistory> {
        return blurEventConverter.convertChangeNonce(log, timestamp)
    }
}
