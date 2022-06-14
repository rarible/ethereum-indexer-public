package com.rarible.protocol.order.listener.service.order

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import java.math.BigInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class ApprovalOrdersTest: AbstractIntegrationTest() {

    @Test
    internal fun `should make order inactive if not approve`() {
        runBlocking {
            val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

            val owner = Address.apply(Keys.getAddressFromPrivateKey(privateKey))


            val userSender = MonoSigningTransactionSender(
                ethereum,
                MonoSimpleNonceProvider(ethereum),
                privateKey,
                BigInteger.valueOf(8000000)
            ) { Mono.just(BigInteger.ZERO) }

            val token = TestERC721.deployAndWait(userSender, poller, "TEST", "TST").awaitFirst()
            token.setApprovalForAll(transferProxyAddresses.transferProxy, true).execute().verifySuccess()
            token.setApprovalForAll(transferProxyAddresses.transferProxy, false).execute().verifySuccess()
            token.setApprovalForAll(AddressFactory.create(), true).execute().verifySuccess()

            val order = createOrder().copy(maker = owner, make = Asset(Erc721AssetType(token = token.address(), tokenId = EthUInt256.ONE), EthUInt256.ONE))
            val version = OrderVersion(
                maker = order.maker,
                make = order.make,
                take = order.take,
                type = order.type,
                data = order.data,
                salt = order.salt,
                end = order.end,
                makePrice = order.makePrice,
                takePrice = order.takePrice,
                taker = order.taker,
                makePriceUsd = order.makePriceUsd,
                takePriceUsd = order.takePriceUsd,
                makeUsd = order.makeUsd,
                takeUsd = order.takeUsd,
                onChainOrderKey = null,
                start = order.start,
                signature = order.signature
            )

            val saved = orderUpdateService.save(version)
            Wait.waitAssert {
                val history = approvalHistoryRepository.findAll().toList()
                assertThat(history).hasSize(2)

                orderReduceService.updateOrder(Word.apply(saved.hash))

                val updated = orderRepository.findById(Word.apply(saved.hash))
                assertThat(updated).isNotNull
                assertThat(updated!!.status).isEqualTo(OrderStatus.INACTIVE)
            }
        }
    }
}
