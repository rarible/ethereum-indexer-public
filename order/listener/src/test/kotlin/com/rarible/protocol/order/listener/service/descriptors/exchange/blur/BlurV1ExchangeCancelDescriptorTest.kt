package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.order.core.model.OrderCancel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BlurV1ExchangeCancelDescriptorTest : AbstractBlurDescriptorTest() {
    private val subscriber = BlurV1ExchangeCancelDescriptor(contractsProvider, blurEventConverter)

    @Test
    fun `convert cancel`() = runBlocking<Unit> {
        val cancels = listOf<OrderCancel>(mockk(), mockk())

        checkConversion(subscriber, cancels) { log, transaction, index, totalLogs, date ->
            coEvery { blurEventConverter.convertToCancel(log, transaction, index, totalLogs, date) } returns cancels
        }
    }
}
