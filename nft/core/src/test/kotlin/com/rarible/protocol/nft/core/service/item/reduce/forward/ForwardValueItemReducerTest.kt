package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class ForwardValueItemReducerTest {
    private val tokenRegistrationService = mockk<TokenRegistrationService>()
    private val forwardValueItemReducer = ForwardValueItemReducer(tokenRegistrationService)
    @Test
    fun `should calculate supply on transfer event with no supply`() = runBlocking<Unit> {
        every { tokenRegistrationService.getTokenStandard(any()) } returns Mono.just(TokenStandard.ERC721)
        val item = createRandomItem().copy(supply = EthUInt256.ZERO)
        val event = createRandomTransferItemEvent().copy(value = EthUInt256.of(1))

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should not calculate supply on transfer event with supply`() = runBlocking<Unit> {
        every { tokenRegistrationService.getTokenStandard(any()) } returns Mono.just(TokenStandard.ERC721)
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        val event = createRandomTransferItemEvent().copy(value = EthUInt256.of(1))

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should calculate supply on mint event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.ONE)
        val event = createRandomMintItemEvent().copy(supply = EthUInt256.of(9))

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should calculate supply on burn event`() = runBlocking<Unit> {
        val item = createRandomItem().copy(supply = EthUInt256.of(11))
        val event = createRandomBurnItemEvent().copy(supply = EthUInt256.ONE)

        val reducedItem = forwardValueItemReducer.reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
    }
}
