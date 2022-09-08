package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.transaction.ReadOnlyMonoTransactionSender

internal class SudoSwapPoolCollectionProviderTest {
    private val sender = mockk<ReadOnlyMonoTransactionSender>()
    private val sudoSwapEventConverter = mockk<SudoSwapEventConverter>()
    private val orderRepository = mockk<OrderRepository>()

    private val sudoSwapPoolCollectionProvider = SudoSwapPoolCollectionProvider(
        sender = sender,
        sudoSwapEventConverter = sudoSwapEventConverter,
        orderRepository = orderRepository
    )

    @Test
    fun `should get collection from order`() = runBlocking<Unit> {
        val poolAddress = randomAddress()
        val orderHash = Word.apply(randomWord())
        val collection = randomAddress()
        val order = createSellOrder().copy(make = randomAmmNftAsset(collection))
        every { sudoSwapEventConverter.getPoolHash(poolAddress) } returns orderHash
        coEvery { orderRepository.findById(orderHash) } returns order

        val result = sudoSwapPoolCollectionProvider.getPoolCollection(poolAddress)
        assertThat(result).isEqualTo(collection)
    }
}