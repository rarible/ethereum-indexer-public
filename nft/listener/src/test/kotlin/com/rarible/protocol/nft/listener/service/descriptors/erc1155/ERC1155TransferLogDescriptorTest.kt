package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.log
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.item.CustomMintDetector
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction

internal class ERC1155TransferLogDescriptorTest {

    private val tokenService = mockk<TokenService>()
    private val customMintDetector = mockk<CustomMintDetector>()
    private val indexerProperties = mockk<NftIndexerProperties> {
        every { scannerProperties } returns NftIndexerProperties.ScannerProperties()
    }
    private val ignoredTokenResolver = mockk<IgnoredTokenResolver> {
        every { resolve() } returns emptySet()
    }
    private val descriptor = ERC1155TransferLogDescriptor(
        tokenService,
        customMintDetector,
        ignoredTokenResolver,
        indexerProperties
    )

    @Test
    fun `convert - ok, with 2 topics event`() = runBlocking<Unit> {
        val token = randomAddress()
        val log = log(
            listOf(
                Word.apply("0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62"),
                Word.apply("0x00000000000000000000000042cfaae209149007f304c51af1a0c985c292f740"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
            ),
            "0x00000000000000000000000042cfaae209149007f304c51af1a0c985c292f7400000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000a",
            address = token,
        )
        coEvery { tokenService.getTokenStandard(token) } returns TokenStandard.ERC1155
        every { customMintDetector.isErc1155Mint(any(), any()) } returns false
        val transaction = mockk<Transaction>()
        val event = descriptor.convert(log, transaction, 0, 0, 0).awaitSingle()
        assertThat(event.from).isEqualTo(Address.ZERO())
        assertThat(event.owner).isEqualTo(Address.apply("0x42cfaAe209149007f304c51AF1a0c985c292f740"))
        assertThat(event.tokenId).isEqualTo(EthUInt256.of(2))
        assertThat(event.value).isEqualTo(EthUInt256.of(10))
    }
}
