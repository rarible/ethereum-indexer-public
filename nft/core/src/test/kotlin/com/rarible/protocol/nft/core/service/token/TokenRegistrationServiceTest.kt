package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.contracts.erc20.test.SimpleERC20
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class TokenRegistrationServiceTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var tokenRegistrationService: TokenRegistrationService

    @Test
    fun `register ERC721 token`() = runBlocking<Unit> {
        val adminSender = newSender().second
        val erc721 = ERC721Rarible.deployAndWait(adminSender, poller).awaitFirst()
        erc721.__ERC721Rarible_init(
            "Name",
            "Symbol",
            "baseURI",
            "contractURI"
        ).execute().verifySuccess()
        val token = tokenRegistrationService.register(erc721.address()).awaitFirst()
        val expectedToken = Token(
            id = erc721.address(),
            name = "Name",
            symbol = "Symbol",
            owner = adminSender.from(),
            status = ContractStatus.CONFIRMED,
            features = setOf(
                TokenFeature.APPROVE_FOR_ALL,
                TokenFeature.BURN,
                TokenFeature.MINT_AND_TRANSFER
            ),
            standard = TokenStandard.ERC721
        )
        assertThat(token).isEqualToIgnoringGivenFields(
            expectedToken,
            Token::lastEventId.name,
            Token::version.name,
            Token::dbUpdatedAt.name
        )
    }

    @Test
    fun `register ERC20 token by checking interface`() = runBlocking<Unit> {
        val adminSender = newSender().second
        val erc20 = SimpleERC20.deployAndWait(adminSender, poller).awaitFirst()

        val token = tokenRegistrationService.register(erc20.address()).awaitFirst()
        assertThat(token.standard).isEqualTo(TokenStandard.ERC20)
    }

    @Test
    fun `register ERC20 token by checking bytecode`() = runBlocking<Unit> {
        val adminSender = newSender().second
        val erc20 = SimpleERC20.deployAndWait(adminSender, poller).awaitFirst()

        val standard = tokenRegistrationService.fetchTokenStandardByFunctionSignatures(adminSender, erc20.address())
        assertThat(standard).isEqualTo(TokenStandard.ERC20)
    }
}
