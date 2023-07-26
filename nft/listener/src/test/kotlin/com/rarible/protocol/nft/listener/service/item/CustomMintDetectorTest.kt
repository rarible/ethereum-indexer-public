package com.rarible.protocol.nft.listener.service.item

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.core.test.data.randomAddress
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

class CustomMintDetectorTest {

    private val customMintDetector = CustomMintDetector()

    @Test
    fun `should detect 1155 mint by mint method id`() {
        val event = mockk<TransferSingleEvent> {
            every { _from() } returns Address.ONE()
            every { _to() } returns Address.ONE()
        }
        val transaction = mockk<Transaction> {
            every { input() } returns CustomMintDetector.MINT_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isErc1155Mint(event, transaction)).isTrue
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
        assertThat(customMintDetector.isErc1155Mint(event, transaction)).isTrue
    }

    @Test
    fun `should detect mint by polygon mint method id`() {
        val address = randomAddress()
        val event = mockk<TransferSingleEvent> {
            every { _from() } returns address
        }
        val transaction = mockk<Transaction> {
            every { from() } returns address
            every { input() } returns CustomMintDetector.POLYGON_MINT_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isErc1155Mint(event, transaction)).isTrue
    }

    @Test
    fun `should detect mint by distribute nft method id`() {
        val token = randomAddress()
        val log = mockk<Log> {
            every { address() } returns token
        }
        val event = mockk<TransferSingleEvent> {
            every { _from() } returns token
            every { log() } returns log
        }
        val transaction = mockk<Transaction> {
            every { input() } returns CustomMintDetector.DISTRIBUTE_NFT_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isErc1155Mint(event, transaction)).isTrue
    }

    @Test
    fun `should detect 721 mint by distribute nft method id`() {
        val token = randomAddress()
        val log = mockk<Log> {
            every { address() } returns token
        }
        val event = mockk<TransferEvent> {
            every { from() } returns token
            every { log() } returns log
        }
        val transaction = mockk<Transaction> {
            every { input() } returns CustomMintDetector.DISTRIBUTE_NFT_METHOD_ID_SIGNATURE
        }
        assertThat(customMintDetector.isErc721Mint(event, transaction)).isTrue
    }
}
