package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.EventAwareBaseTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@End2EndTest
class PendingTransactionFt : EventAwareBaseTest() {
    @Test
    fun `should create empty pending transaction`() = runBlocking<Unit> {
        val request = CreateTransactionRequestDto(
            hash = Word.apply(randomWord()),
            from = randomAddress(),
            input = Binary.empty(),
            nonce = randomLong(),
            to = randomAddress()
        )
        val transactions = nftTransactionApiClient.createNftPendingTransaction((request)).collectList().awaitFirst()
        assertThat(transactions).isEmpty()
    }
}