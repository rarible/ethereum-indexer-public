package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.order.core.model.OrderSideMatch
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BlurV1ExchangeDescriptorTest : AbstractBlurDescriptorTest() {
    private val subscriber = BlurV1ExchangeDescriptor(contractsProvider, blurEventConverter)

    @Test
    fun `convert cancel`() = runBlocking<Unit> {
        val matches = listOf<OrderSideMatch>(mockk(), mockk())

        checkConversion(subscriber, matches) { log, transaction, index, totalLogs, date ->
            coEvery { blurEventConverter.convertToSideMatch(log, transaction, index, totalLogs, date,) } returns matches
        }
    }
}
