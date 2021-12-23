package com.rarible.protocol.block.scanner

import com.rarible.protocol.block.scanner.test.AbstractIntegrationTest
import com.rarible.protocol.block.scanner.test.IntegrationTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

@IntegrationTest
@DelicateCoroutinesApi
internal class BlockIndexerApplicationFt : AbstractIntegrationTest() {
    @Test
    fun `should scan from first block`() = runBlocking<Unit> {
        val (account, _) = newSender()
        val transaction = depositTransaction(account, BigInteger.valueOf(10000))

        checkBlockEventWasPublished {
            assertThat(this.number).isLessThanOrEqualTo(transaction.blockNumber().longValueExact())
        }
    }
}
