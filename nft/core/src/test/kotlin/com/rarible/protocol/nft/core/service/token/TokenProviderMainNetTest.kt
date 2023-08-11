package com.rarible.protocol.nft.core.service.token

import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenByteCodeRepository
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.time.Duration

@Disabled
class TokenProviderMainNetTest {

    private val sender = createSender()
    private val tokenByteCodeProvider = TokenByteCodeProvider(sender, 3, 5, 1, Duration.ofMinutes(1))
    private val tokenByteCodeRepository = mockk<TokenByteCodeRepository>()
    private val featureFlags = FeatureFlags(saveTokenByteCode = false)
    private val tokenByteCodeService = TokenByteCodeService(tokenByteCodeProvider, tokenByteCodeRepository, featureFlags)
    private val service = TokenProvider(sender, tokenByteCodeService, emptyList())

    private fun createSender() = ReadOnlyMonoTransactionSender(
        MonoEthereum(
            WebClientTransport(
                "https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/",
                MonoEthereum.mapper(),
                10000,
                10000
            )
        ),
        Address.ZERO()
    )

    private val expectedStandards = listOf(
        // Mythereum CARD (no ERC165 supportsInterface)
         "0xc70be5b7c19529ef642d16c10dfe91c58b5c3bf0" to TokenStandard.ERC721,

        // ENS domains
        "0x57f1887a8bf19b14fc0df6fd9b2acc9af147ea85" to TokenStandard.ERC721,

        // https://rarible.com/parallel
        "0x76be3b62873462d2142405439777e971754e8e77" to TokenStandard.ERC1155,

        // CryptoKitties
        "0x06012c8cf97BEaD5deAe237070F9587f8E7A266d" to TokenStandard.DEPRECATED,

        // EtherTulips
        "0x995020804986274763df9deb0296b754f2659ca1" to TokenStandard.ERC721,

        // OWEFFPUNKS (LEP)
        "0x0b09176b669a3642b8090e207e8e7557f868479e" to TokenStandard.ERC721,

        // Divine Anarchy https://etherscan.io/address/0xc631164b6cb1340b5123c9162f8558c866de1926
        // Its 'supportsInterface' is calculated for a subset of the common ERC721,
        // although the contract defines all the necessary methods.
        "0xc631164B6CB1340B5123c9162f8558c866dE1926" to TokenStandard.ERC721
    )

    @Test
    @Disabled
    fun `request token standards`() = runBlocking<Unit> {
        val errors = arrayListOf<Pair<Address, TokenStandard>>()
        for ((address, expectedStandard) in expectedStandards) {
            println("Processing $address (https://etherscan.io/token/$address)")
            val token = Address.apply(address)
            val standard = service.fetchTokenStandard(token)
            if (standard != expectedStandard) {
                println("Invalid standard of $token: $standard instead of $expectedStandard")
                errors += token to standard
            } else {
                println("Standard of $token is correct: $expectedStandard")
            }
        }
        assertThat(errors).isEmpty()
    }
}
