package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.contracts.erc20.test.SimpleERC20
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class TokenProviderIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenProvider: TokenProvider

    @Test
    fun `get standard - by bytecode`() = runBlocking<Unit> {
        val adminSender = newSender().second
        val erc20 = SimpleERC20.deployAndWait(adminSender, poller).awaitFirst()

        val standard = tokenProvider.fetchTokenStandardBySignature(erc20.address())
        assertThat(standard).isEqualTo(TokenStandard.ERC20)
    }
}
