package com.rarible.protocol.nft.listener.service.item

import com.rarible.contracts.erc1155.TransferSingleEvent
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction

internal class CustomMintDetectorTest {
    private val customMintDetector = CustomMintDetector()

    @Test
    fun `should detect mint by mint method id`() {
        val event = mockk<TransferSingleEvent> {
            every { _from() } returns Address.ONE()
            every { _to() } returns Address.ONE()
        }
        val transaction = mockk<Transaction> {
            every { input() } returns CustomMintDetector.MINT_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isMint(event, transaction)).isTrue
    }

    @Test
    fun `should detect mint by airdrop method id`() {
        val event = mockk<TransferSingleEvent> {
            every { _from() } returns Address.ONE()
        }
        val transaction = mockk<Transaction> {
            every { from() } returns Address.ONE()
            every { input() } returns CustomMintDetector.AIRDROP_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isMint(event, transaction)).isTrue
    }
}