package com.rarible.protocol.order.core.service.pool

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.randomAmmNftAsset
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.abi.AddressType
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger

internal class PoolInfoProviderTest {
    private val sender = mockk<ReadOnlyMonoTransactionSender>()
    private val orderRepository = mockk<OrderRepository>()
    private val sudoSwapProtocolFeeProvider = mockk<SudoSwapProtocolFeeProvider>()

    private val sudoSwapPoolCollectionProvider = PoolInfoProvider(
        sender = sender,
        orderRepository = orderRepository,
        sudoSwapProtocolFeeProvider = sudoSwapProtocolFeeProvider,
        featureFlags = OrderIndexerProperties.FeatureFlags(),
    )

    @Test
    fun `should get info from order`() = runBlocking<Unit> {
        val poolAddress = randomAddress()
        val orderHash = Word.apply(randomWord())
        val collection = randomAddress()
        val data = createOrderSudoSwapAmmDataV1()
        val protocolFee = randomBigInt()
        val order = createSellOrder().copy(make = randomAmmNftAsset(collection), data = data, type = OrderType.AMM)

        coEvery { orderRepository.findById(orderHash) } returns order
        coEvery { sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory) } returns protocolFee

        val result = sudoSwapPoolCollectionProvider.getPollInfo(orderHash, poolAddress)!!
        Assertions.assertThat(result.collection).isEqualTo(collection)
        Assertions.assertThat(result.curve).isEqualTo(data.bondingCurve)
        Assertions.assertThat(result.spotPrice).isEqualTo(data.spotPrice)
        Assertions.assertThat(result.delta).isEqualTo(data.delta)
        Assertions.assertThat(result.fee).isEqualTo(data.fee)
        Assertions.assertThat(result.protocolFee).isEqualTo(protocolFee)
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
        val factory = randomAddress()
        val fee = randomBigInt()
        val protocolFee = randomBigInt()
        val pairVariant = BigInteger.ONE

        coEvery { orderRepository.findById(orderHash) } returns null
        coEvery { sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(factory) } returns protocolFee

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.nftSignature().id(),
                    null
                )
            )
        } returns Mono.just(AddressType.encode(collection))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.spotPriceSignature().id(),
                    null
                )
            )
        } returns Mono.just(Uint256Type.encode(spotPrice))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.deltaSignature().id(),
                    null
                )
            )
        } returns Mono.just(Uint256Type.encode(delta))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.bondingCurveSignature().id(),
                    null
                )
            )
        } returns Mono.just(AddressType.encode(curve))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.factorySignature().id(),
                    null
                )
            )
        } returns Mono.just(AddressType.encode(factory))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.pairVariantSignature().id(),
                    null
                )
            )
        } returns Mono.just(Uint256Type.encode(pairVariant))

        coEvery {
            sender.call(
                Transaction(
                    poolAddress,
                    null,
                    null,
                    null,
                    null,
                    LSSVMPairV1.feeSignature().id(),
                    null
                )
            )
        } returns Mono.just(Uint256Type.encode(fee))

        val result = sudoSwapPoolCollectionProvider.getPollInfo(orderHash, poolAddress)!!
        Assertions.assertThat(result.collection).isEqualTo(collection)
        Assertions.assertThat(result.curve).isEqualTo(curve)
        Assertions.assertThat(result.spotPrice).isEqualTo(spotPrice)
        Assertions.assertThat(result.delta).isEqualTo(delta)
        Assertions.assertThat(result.fee).isEqualTo(fee)
        Assertions.assertThat(result.protocolFee).isEqualTo(protocolFee)
        Assertions.assertThat(result.token).isEqualTo(Address.ZERO())
    }
}
