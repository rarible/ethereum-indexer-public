package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class ERC1155TransferDescriptorTest : AbstractIntegrationTest() {

    @ParameterizedTest
    @EnumSource(ReduceVersion::class)
    fun convert(version: ReduceVersion) = withReducer(version) {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val token = TestERC1155.deployAndWait(
            userSender,
            poller,
            ""
        ).awaitFirst()

        val itemId = ItemId(token.address(), EthUInt256(BigInteger.ONE))
        token.mint(userSender.from(), itemId.tokenId.value, BigInteger.valueOf(5), ByteArray(0)).execute().verifySuccess()

        Wait.waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(token.address(), tokenId = null)
                .filter { it.log.topic in ItemType.TRANSFER.topic }
                .collectList().awaitFirst()

            assertThat(transfers).hasSize(1)
        }
        Wait.waitAssert {
            val savedNft = tokenRepository.findById(itemId.token).awaitFirstOrNull()
            assertThat(savedNft).isNotNull

            assertThat(savedNft!!.id).isEqualTo(token.address())
            assertThat(savedNft.standard).isEqualTo(TokenStandard.ERC1155)

            val savedNftItem = itemRepository.findById(itemId).awaitFirstOrNull()
            assertThat(savedNftItem).isNotNull

            assertThat(savedNftItem!!.token).isEqualTo(token.address())
            assertThat(savedNftItem.supply).isEqualTo(EthUInt256.of(5))

            checkActivityWasPublished(savedNftItem.token, savedNftItem.tokenId, TransferSingleEvent.id(), MintDto::class.java)
        }
    }
}
