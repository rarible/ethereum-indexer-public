package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class PendingTransactionServiceTest : AbstractIntegrationTest() {
    @Test
    fun `should create empty pending transaction`() = runBlocking<Unit> {
        val request = CreateTransactionRequestDto(
            hash = Word.apply(randomWord()),
            from = randomAddress(),
            input = Binary.empty(),
            nonce = randomLong(),
            to = randomAddress()
        )
        val transactions = transactionApi.createOrderPendingTransaction(request).collectList().awaitFirst()
        assertThat(transactions).isEmpty()
    }
}
