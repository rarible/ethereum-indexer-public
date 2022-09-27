package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.abi.AddressType
import scalether.abi.Uint256Type
import scalether.domain.request.Transaction
import scalether.transaction.ReadOnlyMonoTransactionSender

internal class SudoSwapPoolInfoProviderTest {
    private val sender = mockk<ReadOnlyMonoTransactionSender>()
    private val sudoSwapEventConverter = mockk<SudoSwapEventConverter>()
    private val orderRepository = mockk<OrderRepository>()

    private val sudoSwapPoolCollectionProvider = SudoSwapPoolInfoProvider(
        sender = sender,
        sudoSwapEventConverter = sudoSwapEventConverter,
        orderRepository = orderRepository
    )

    @Test
    fun `should get info from order`() = runBlocking<Unit> {
        val poolAddress = randomAddress()
        val orderHash = Word.apply(randomWord())
        val collection = randomAddress()
        val data = createOrderSudoSwapAmmDataV1()
        val order = createSellOrder().copy(make = randomAmmNftAsset(collection), data = data, type = OrderType.AMM)

        every { sudoSwapEventConverter.getPoolHash(poolAddress) } returns orderHash
        coEvery { orderRepository.findById(orderHash) } returns order

        val result = sudoSwapPoolCollectionProvider.gePollInfo(poolAddress)
        assertThat(result.collection).isEqualTo(collection)
        assertThat(result.curve).isEqualTo(data.bondingCurve)
        assertThat(result.spotPrice).isEqualTo(data.spotPrice)
        assertThat(result.delta).isEqualTo(data.delta)
    }

    @Test
    @Suppress("ReactiveStreamsUnusedPublisher")
    fun `should get info from chain`() = runBlocking<Unit> {
        val poolAddress = randomAddress()
        val orderHash = Word.apply(randomWord())
        val collection = randomAddress()
        val curve = randomAddress()
        val spotPrice = randomBigInt()
        val delta = randomBigInt()

        every { sudoSwapEventConverter.getPoolHash(poolAddress) } returns orderHash
        coEvery { orderRepository.findById(orderHash) } returns null

        coEvery {
            sender.call(Transaction(
                poolAddress,
                null,
                null,
                null,
                null,
                LSSVMPairV1.nftSignature().id(),
                null
            ))
        } returns Mono.just(AddressType.encode(collection))

        coEvery {
            sender.call(Transaction(
                poolAddress,
                null,
                null,
                null,
                null,
                LSSVMPairV1.spotPriceSignature().id(),
                null
            ))
        } returns Mono.just(Uint256Type.encode(spotPrice))

        coEvery {
            sender.call(Transaction(
                poolAddress,
                null,
                null,
                null,
                null,
                LSSVMPairV1.deltaSignature().id(),
                null
            ))
        } returns Mono.just(Uint256Type.encode(delta))

        coEvery {
            sender.call(Transaction(
                poolAddress,
                null,
                null,
                null,
                null,
                LSSVMPairV1.bondingCurveSignature().id(),
                null
            ))
        } returns Mono.just(AddressType.encode(curve))

        val result = sudoSwapPoolCollectionProvider.gePollInfo(poolAddress)
        assertThat(result.collection).isEqualTo(collection)
        assertThat(result.curve).isEqualTo(curve)
        assertThat(result.spotPrice).isEqualTo(spotPrice)
        assertThat(result.delta).isEqualTo(delta)
    }
}