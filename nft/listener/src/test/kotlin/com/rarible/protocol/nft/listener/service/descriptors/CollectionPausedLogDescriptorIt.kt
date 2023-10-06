package com.rarible.protocol.nft.listener.service.descriptors

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import com.rarible.protocol.nft.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3jold.crypto.Keys
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class CollectionPausedLogDescriptorIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var testCollectionHandler: TestKafkaHandler<NftCollectionEventDto>

    @Test
    fun `pause unpause token`() = runBlocking<Unit> {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        val contract = TestERC1155.deployAndWait(userSender, poller).awaitFirst()
        val itemId = ItemId(contract.address(), EthUInt256(BigInteger.ONE))
        contract.mint(userSender.from(), itemId.tokenId.value, BigInteger.valueOf(5)).execute().verifySuccess()

        Wait.waitAssert {
            val token = tokenService.getToken(contract.address())
            assertThat(token).isNotNull
            val event = testCollectionHandler.events.remove() as NftCollectionUpdateEventDto
            assertThat(event.collection.id).isEqualTo(contract.address())
            assertThat(event.collection.flags).isNull()
        }

        contract.emitPauseEvent(true).execute().verifySuccess()

        Wait.waitAssert {
            val token = tokenService.getToken(contract.address())
            assertThat(token).isNotNull
            assertThat(token!!.flags?.paused).isTrue()
            val event = testCollectionHandler.events.remove() as NftCollectionUpdateEventDto
            assertThat(event.collection.id).isEqualTo(contract.address())
            assertThat(event.collection.flags?.paused).isTrue()
        }

        contract.emitPauseEvent(false).execute().verifySuccess()

        Wait.waitAssert {
            val token = tokenService.getToken(contract.address())
            assertThat(token).isNotNull
            assertThat(token!!.flags?.paused).isFalse()
            val event = testCollectionHandler.events.remove() as NftCollectionUpdateEventDto
            assertThat(event.collection.id).isEqualTo(contract.address())
            assertThat(event.collection.flags?.paused).isFalse()
        }
    }
}
