package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemType
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.mongodb.core.findAll
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import kotlin.random.Random

@FlowPreview
@IntegrationTest
class ERC1155TransferBatchDescriptorTest : AbstractIntegrationTest() {

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

        token.mint(userSender.from(), BigInteger.ONE, BigInteger.valueOf(5), ByteArray(0)).execute().verifySuccess()
        val to = Address.apply(Random.nextBytes(20))
        token.safeBatchTransferFrom(userSender.from(), to, arrayOf(BigInteger.ONE), arrayOf(BigInteger.ONE), ByteArray(0)).execute().verifySuccess()

        Wait.waitAssert {
            val transfers = nftItemHistoryRepository
                .findItemsHistory(token.address(), tokenId = null)
                .filter { it.log.topic in ItemType.TRANSFER.topic }
                .collectList().awaitFirst()

            Assertions.assertThat(transfers).hasSize(2)
        }

        Wait.waitAssert {
            val savedNftTokens = tokenRepository.findAll().collectList().awaitFirst()
            Assertions.assertThat(savedNftTokens).hasSize(1)

            val savedNft = savedNftTokens.single()
            Assertions.assertThat(savedNft.id).isEqualTo(token.address())
            Assertions.assertThat(savedNft.standard).isEqualTo(TokenStandard.ERC1155)

            val savedNftItems = mongo.findAll<Item>().collectList().awaitFirst()
            Assertions.assertThat(savedNftItems).hasSize(1)

            val savedNftItem = savedNftItems.single()
            Assertions.assertThat(savedNftItem.token).isEqualTo(token.address())
            Assertions.assertThat(savedNftItem.supply).isEqualTo(EthUInt256.of(5))

            checkActivityWasPublished(savedNftItem.token, savedNftItem.tokenId, TransferBatchEvent.id(), TransferDto::class.java)
        }
    }
}
