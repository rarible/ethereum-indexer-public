package com.rarible.protocol.nft.listener.service.descriptors.royalty

import com.rarible.core.common.filterIsInstance
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.royalties.SecondarySaleFeesEvent
import com.rarible.protocol.contracts.test.royalties.v1.RoyaltiesV1TestImpl
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple2
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class RoyaltyV1LogDescriptorTest : AbstractIntegrationTest() {

    @Test
    fun convert() = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        val royaltyContract = RoyaltiesV1TestImpl.deployAndWait(userSender, poller).awaitFirst()

        val tokenId = EthUInt256.of((1L..1000L).random())
        val royalty1 = Tuple2.apply(AddressFactory.create(), BigInteger.ONE)
        val royalty2 = Tuple2.apply(AddressFactory.create(), BigInteger.TEN)

        royaltyContract.saveRoyalties(tokenId.value, listOf(royalty1, royalty2).toTypedArray()).execute()
            .verifySuccess()

        Wait.waitAssert {
            val royalties = nftItemHistoryRepository
                .findItemsHistory(royaltyContract.address(), tokenId = tokenId)
                .filter { it.log.topic == SecondarySaleFeesEvent.id() }
                .map { it.item }
                .filterIsInstance<ItemRoyalty>()
                .collectList().awaitFirst()

            Assertions.assertThat(royalties).hasSize(1)
            Assertions.assertThat(royalties.first().royalties).containsExactly(royalty1.toPart(), royalty2.toPart())
        }
    }

    private fun Tuple2<Address, BigInteger>.toPart(): Part {
        return Part(_1(), _2().toInt())
    }
}
