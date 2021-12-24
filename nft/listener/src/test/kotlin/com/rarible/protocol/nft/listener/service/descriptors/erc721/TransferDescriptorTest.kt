package com.rarible.protocol.nft.listener.service.descriptors.erc721

import com.rarible.contracts.erc721.TransferEvent
import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class TransferDescriptorTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun convert(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = TestERC721.deployAndWait(userSender, poller, "TEST", "TST").awaitFirst()
        val itemId = ItemId(token.address(), EthUInt256(BigInteger.ONE))
        token.mint(userSender.from(), itemId.tokenId.value,"").execute().verifySuccess()

        Wait.waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(token.address(), tokenId = null)
                .filter { it.log.topic in ItemType.TRANSFER.topic }
                .collectList().awaitFirst()

            assertThat(transfers).hasSize(1)
        }
        Wait.waitAssert {
            val savedNftItem = itemRepository.findById(itemId).awaitFirstOrNull()
            assertThat(savedNftItem).isNotNull

            assertThat(savedNftItem!!.token).isEqualTo(token.address())
            assertThat(savedNftItem.supply).isEqualTo(EthUInt256.of(1))

            val savedNft = tokenRepository.findById(itemId.token).awaitFirstOrNull()
            assertThat(savedNft).isNotNull

            assertThat(savedNft!!.id).isEqualTo(token.address())
            assertThat(savedNft.standard).isEqualTo(TokenStandard.ERC721)
            assertThat(savedNft.features)
                .contains(TokenFeature.APPROVE_FOR_ALL)
                .doesNotContain(TokenFeature.MINT_AND_TRANSFER)

            checkActivityWasPublished(savedNftItem.token, savedNftItem.tokenId, TransferEvent.id(), MintDto::class.java)
        }
    }
}
