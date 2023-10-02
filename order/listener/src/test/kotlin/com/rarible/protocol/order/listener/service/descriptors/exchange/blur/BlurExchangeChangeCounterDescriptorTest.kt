package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.protocol.order.core.model.ChangeNonceHistory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BlurExchangeChangeCounterDescriptorTest : AbstractBlurDescriptorTest() {
    private val subscriber =
        BlurExchangeChangeCounterDescriptor(contractsProvider, blurEventConverter, autoReduceService)

    @Test
    fun `convert change nonce`() = runBlocking<Unit> {
        val changes = listOf<ChangeNonceHistory>(mockk(), mockk())

        checkConversion(subscriber, changes) { log, _, _, _, date ->
            coEvery { blurEventConverter.convertChangeNonce(log, date) } returns changes
        }
    }
}
