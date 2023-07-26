package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

internal class ReduceSkipTokensTest {
    private val skipToken1 = ItemId(AddressFactory.create(), EthUInt256.ONE)
    private val skipToken2 = ItemId(AddressFactory.create(), EthUInt256.TEN)

    @Test
    fun `should not skip for the first time`() {
        val reduceSkipTokens = ReduceSkipTokens(listOf(skipToken1, skipToken2))

        assertThat(reduceSkipTokens.allowReducing(skipToken1.token, skipToken1.tokenId)).isTrue()
        assertThat(reduceSkipTokens.allowReducing(skipToken2.token, skipToken2.tokenId)).isTrue()
        assertThat(reduceSkipTokens.allowReducing(AddressFactory.create(), EthUInt256.of((1L..1000).random()))).isTrue()
    }

    @Test
    fun `should skip for the second and more time`() {
        val reduceSkipTokens = ReduceSkipTokens(listOf(skipToken1, skipToken2))

        reduceSkipTokens.allowReducing(skipToken1.token, skipToken1.tokenId)
        reduceSkipTokens.allowReducing(skipToken2.token, skipToken2.tokenId)
        for (i in 1..10) {
            assertThat(reduceSkipTokens.allowReducing(skipToken1.token, skipToken1.tokenId)).isFalse()
            assertThat(reduceSkipTokens.allowReducing(skipToken2.token, skipToken2.tokenId)).isFalse()
            assertThat(reduceSkipTokens.allowReducing(AddressFactory.create(), EthUInt256.of((1L..1000).random()))).isTrue()
        }
    }

    @Test
    fun `should allow after period`() {
        val reduceSkipTokens = ReduceSkipTokens(listOf(skipToken1, skipToken2))

        for (i in (ReduceSkipTokens.ALLOWING_PERIOD - 1)..(ReduceSkipTokens.ALLOWING_PERIOD * 10)) {
            if (i / ReduceSkipTokens.ALLOWING_PERIOD == 0L) {
                assertThat(reduceSkipTokens.allowReducing(skipToken1.token, skipToken1.tokenId)).isTrue()
                assertThat(reduceSkipTokens.allowReducing(skipToken2.token, skipToken2.tokenId)).isTrue()
            } else {
                assertThat(reduceSkipTokens.allowReducing(skipToken1.token, skipToken1.tokenId)).isFalse()
                assertThat(reduceSkipTokens.allowReducing(skipToken2.token, skipToken2.tokenId)).isFalse()
            }
        }
    }
}
