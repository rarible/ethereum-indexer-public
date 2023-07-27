package com.rarible.protocol.order.core.service.pool

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.isPoolCreate
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.data.randomPoolNftDeposit
import com.rarible.protocol.order.core.data.randomPoolNftWithdraw
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.util.stream.Stream

internal class PoolEventListenerTest {
    private val orderRepository = mockk<OrderRepository>()
    private val orderPublisher = mockk<ProtocolOrderPublisher>()

    val listener = PoolOrderEventListener(orderRepository, orderPublisher)

    private companion object {
        @JvmStatic
        fun poolHistory(): Stream<Arguments> {
            val collection = randomAddress()
            val tokenIds = (1..10).map { EthUInt256.of(randomInt()) }
            val itemIds = tokenIds.map { ItemId(collection, it.value).toString() }
            return Stream.of(
                Arguments.of(randomSellOnChainAmmOrder().copy(collection = collection, tokenIds = tokenIds), collection, itemIds, true),
                Arguments.of(randomPoolNftDeposit().copy(collection = collection, tokenIds = tokenIds), collection, itemIds, true),
                Arguments.of(randomPoolTargetNftIn().copy(tokenIds = tokenIds), collection, itemIds, true),
                Arguments.of(randomPoolTargetNftOut().copy(tokenIds = tokenIds), collection, itemIds, false),
                Arguments.of(randomPoolNftWithdraw().copy(collection = collection, tokenIds = tokenIds), collection, itemIds, false),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("poolHistory")
    fun `should publish with in nft`(
        poolHistory: PoolHistory,
        collection: Address,
        expectedItemIds: List<String>,
        inNft: Boolean
    ) = runBlocking<Unit> {
        val order = createSellOrder().copy(make = randomAmmNftAsset(collection))
        val logEvent = logEvent(poolHistory)

        coEvery { orderRepository.findById(poolHistory.hash) } returns if (poolHistory.isPoolCreate()) null else order
        coEvery { orderPublisher.publish(any<OrderEventDto>()) } returns Unit

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify { orderPublisher.publish(withArg<OrderEventDto> { eventDto ->
            eventDto as AmmOrderNftUpdateEventDto
            assertThat(eventDto.orderId).isEqualTo(poolHistory.hash.toString())
            assertThat(eventDto.eventId).isEqualTo(logEvent.id)
            if (inNft) {
                assertThat(eventDto.inNft).isEqualTo(expectedItemIds)
                assertThat(eventDto.outNft).isEmpty()
            } else {
                assertThat(eventDto.outNft).isEqualTo(expectedItemIds)
                assertThat(eventDto.inNft).isEmpty()
            }
        }) }
    }

    @Test
    fun `should publish on reverted OnChainAmmOrder`() = runBlocking<Unit> {
        val event = randomSellOnChainAmmOrder()
        val logEvent = logEvent(event).copy(status = EthereumBlockStatus.REVERTED)

        coEvery { orderRepository.findById(event.hash) } returns createSellOrder()
        coEvery { orderPublisher.publish(any<OrderEventDto>()) } returns Unit

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify { orderPublisher.publish(withArg<OrderEventDto> { eventDto ->
            eventDto as AmmOrderNftUpdateEventDto
            assertThat(eventDto.inNft).isEmpty()
            assertThat(eventDto.outNft).hasSize(event.tokenIds.size)
        }) }
    }

    @Test
    fun `should publish not on PoolNftDeposit`() = runBlocking<Unit> {
        val event = randomPoolNftDeposit()
        val order = createSellOrder()
        val logEvent = logEvent(event)

        coEvery { orderRepository.findById(event.hash) } returns order

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify(exactly = 0) { orderPublisher.publish(any<OrderEventDto>()) }
    }

    @Test
    fun `should publish not on PoolNftWithdraw`() = runBlocking<Unit> {
        val event = randomPoolNftWithdraw()
        val order = createSellOrder()
        val logEvent = logEvent(event)

        coEvery { orderRepository.findById(event.hash) } returns order

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify(exactly = 0) { orderPublisher.publish(any<OrderEventDto>()) }
    }

    @Test
    fun `should do nothing on pool update event`() = runBlocking<Unit> {
        val event = randomPoolFeeUpdate()
        val order = createSellOrder()
        val logEvent = logEvent(event)

        coEvery { orderRepository.findById(event.hash) } returns order

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify(exactly = 0) { orderPublisher.publish(any<OrderEventDto>()) }
    }

    private fun logEvent(data: PoolHistory): ReversedEthereumLogRecord {
        return ReversedEthereumLogRecord(
            id = ObjectId().toHexString(),
            data = data,
            address = randomAddress(),
            topic = Word.apply(randomWord()),
            transactionHash = randomWord(),
            status = EthereumBlockStatus.CONFIRMED,
            index = 0,
            logIndex = 0,
            minorLogIndex = 0
        )
    }
}
