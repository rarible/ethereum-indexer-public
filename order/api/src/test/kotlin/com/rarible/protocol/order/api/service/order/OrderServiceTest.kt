package com.rarible.protocol.order.api.service.order

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.EntityNotFoundApiException
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.service.pool.PoolOwnershipService
import com.rarible.protocol.order.core.validator.OrderValidator
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OrderServiceTest {

    @InjectMockKs
    private lateinit var orderService: OrderService

    @MockK
    private lateinit var orderRepository: OrderRepository

    @MockK
    private lateinit var orderUpdateService: OrderUpdateService

    @MockK
    private lateinit var nftItemApiService: NftItemApiService

    @MockK
    private lateinit var poolOwnershipService: PoolOwnershipService

    @MockK
    private lateinit var apiOrderValidator: OrderValidator

    @MockK
    private lateinit var priceUpdateService: PriceUpdateService

    @MockK
    private lateinit var raribleOrderSaveMetric: RegisteredCounter

    @MockK
    private lateinit var poolCurve: PoolCurve

    @MockK
    private lateinit var poolInfoProvider: PoolInfoProvider

    @MockK
    private lateinit var approveService: ApproveService

    @MockK
    private lateinit var commonSigner: CommonSigner

    @MockK
    private lateinit var coreOrderValidator: OrderValidator

    @SpyK
    private var featureFlags: OrderIndexerProperties.FeatureFlags = OrderIndexerProperties.FeatureFlags()

    @Test
    fun `validate and get not found`() = runBlocking<Unit> {
        val order = randomOrder()
        coEvery { orderRepository.findById(order.hash) } returns null

        assertThatExceptionOfType(EntityNotFoundApiException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = randomOrder()
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery { coreOrderValidator.validate(order) } returns Unit

        assertThat(orderService.validateAndGet(order.hash)).isEqualTo(order)
    }

    @Test
    fun `validate and get not valid`() = runBlocking<Unit> {
        val order = randomOrder()
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery { coreOrderValidator.validate(order) } throws OrderDataException("order is not valid")

        assertThatExceptionOfType(OrderDataException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }.withMessage("order is not valid")
    }
}
