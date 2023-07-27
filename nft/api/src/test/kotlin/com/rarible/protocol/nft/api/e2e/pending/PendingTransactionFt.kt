package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.nft.api.test.AbstractIntegrationTest
import com.rarible.protocol.nft.api.test.End2EndTest
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@End2EndTest
class PendingTransactionFt : AbstractIntegrationTest() {

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
