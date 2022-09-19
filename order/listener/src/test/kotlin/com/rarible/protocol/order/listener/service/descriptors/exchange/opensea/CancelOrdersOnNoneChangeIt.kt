package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.service.block.NonceBlockProcessor
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
internal class CancelOrdersOnNoneChangeIt : AbstractOpenSeaV1Test() {
    @Autowired
    private lateinit var nonceHistoryRepository: NonceHistoryRepository

    @Autowired
    private lateinit var nonceBlockProcessor: NonceBlockProcessor

    @Test
    fun `should cancel Seaport maker orders`() = runBlocking {
        val userSender = prepareSender()
        orderIndexerProperties.exchangeContractAddresses.seaportV1 = exchangeV2.address()
        orderIndexerProperties.exchangeContractAddresses.looksrareV1 = Address.ZERO()

        coEvery {
            assetBalanceProvider.getAssetStock(eq(userSender1.from()), any())
        } returns MakeBalanceState(EthUInt256.of(Long.MAX_VALUE))

        val before = nowMillis().minusSeconds(5)

        val orderVersion1 = createOrderVersion().copy(
            maker = userSender.from(),
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = 0, protocol = exchangeV2.address()),
            createdAt = before
        )
        val orderVersion2 = createOrderVersion().copy(
            maker = userSender.from(),
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = 0, protocol = exchangeV2.address()),
            createdAt = before
        )
        val orderVersion3 = createOrderVersion().copy(
            maker = userSender.from(),
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = 0, protocol = exchangeV2.address()),
            createdAt = before
        )
        val orderVersion4 = createOrderVersion().copy(
            maker = userSender.from(),
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = 0, protocol = exchangeV2.address()),
            createdAt = before
        )
        val orderVersion5 = createOrderVersion().copy(
            maker = userSender.from(),
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1().copy(counter = 0),
            createdAt = before
        )

        listOf(orderVersion1, orderVersion2, orderVersion3, orderVersion4, orderVersion5).forEach {
            val order = orderUpdateService.save(it)
            assertThat(order.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        emitEventBySender(userSender)

        Wait.waitAssert {
            val events = nonceHistoryRepository.findAll().toList()
            assertThat(events).hasSize(1)
            val nonceHistory = events.single().data as ChangeNonceHistory

            val savedOrder1 = orderRepository.findById(orderVersion1.hash)!!
            assertThat(savedOrder1.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder1.lastUpdateAt).isEqualTo(nonceHistory.date)

            val savedOrder2 = orderRepository.findById(orderVersion2.hash)!!
            assertThat(savedOrder2.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder2.lastUpdateAt).isEqualTo(nonceHistory.date)

            val savedOrder3 = orderRepository.findById(orderVersion3.hash)!!
            assertThat(savedOrder3.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder3.lastUpdateAt).isEqualTo(nonceHistory.date)

            val savedOrder4 = orderRepository.findById(orderVersion4.hash)!!
            assertThat(savedOrder4.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder4.lastUpdateAt).isEqualTo(nonceHistory.date)

            val savedOrder5 = orderRepository.findById(orderVersion5.hash)!!
            assertThat(savedOrder5.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }

    @Test
    fun `should cancel Looksrare maker orders`() = runBlocking {
        val maker = randomAddress()
        val protocol = randomAddress()
        val date = nowMillis()
        val before = nowMillis().minusSeconds(5)
        val orderVersion1 = createOrderVersion().copy(
            maker = maker,
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1().copy(counter = 0),
            createdAt = before
        )
        val orderVersion2 = createOrderVersion().copy(
            maker = maker,
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1().copy(counter = 0),
            createdAt = before
        )
        val orderVersion3 = createOrderVersion().copy(
            maker = maker,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1().copy(counter = 0),
            createdAt = before
        )
        listOf(orderVersion1, orderVersion2, orderVersion3).forEach {
            val order = orderUpdateService.save(it)
            assertThat(order.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
        orderIndexerProperties.exchangeContractAddresses.looksrareV1 = protocol
        val event = LogEvent(
            data = ChangeNonceHistory(
                maker = maker,
                newNonce = EthUInt256.of(10),
                date = date,
                source = HistorySource.LOOKSRARE
            ),
            address = protocol,
            topic = Word.apply(randomWord()),
            transactionHash = Word.apply(randomWord()),
            status = LogEventStatus.CONFIRMED,
            index = 0,
            logIndex = 0,
            minorLogIndex = 0
        )
        nonceHistoryRepository.save(event)
        nonceBlockProcessor.postProcessLogs(listOf(event)).awaitFirstOrNull()
        Wait.waitAssert {
            val savedOrder1 = orderRepository.findById(orderVersion1.hash)!!
            assertThat(savedOrder1.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder1.lastUpdateAt).isEqualTo(date)

            val savedOrder2 = orderRepository.findById(orderVersion2.hash)!!
            assertThat(savedOrder2.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(savedOrder2.lastUpdateAt).isEqualTo(date)

            val savedOrder3 = orderRepository.findById(orderVersion3.hash)!!
            assertThat(savedOrder3.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }

    private suspend fun prepareSender(): MonoSigningTransactionSender {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        coEvery {
            assetBalanceProvider.getAssetStock(eq(userSender1.from()), any())
        } returns MakeBalanceState(EthUInt256.of(Long.MAX_VALUE))

        return userSender
    }

    private suspend fun emitEventBySender(sender: MonoSigningTransactionSender) {
        exchangeV2.incrementNonce()
            .withSender(sender)
            .execute()
            .verifySuccess()

    }
}