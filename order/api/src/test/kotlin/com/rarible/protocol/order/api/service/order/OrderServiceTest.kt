package com.rarible.protocol.order.api.service.order

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.order.api.data.createOrder
import com.rarible.protocol.order.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.order.api.exceptions.OrderDataException
import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.api.service.order.signature.OrderSignatureResolver
import com.rarible.protocol.order.api.service.order.validation.OrderValidator
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.approve.ApproveService
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.nft.NftItemApiService
import com.rarible.protocol.order.core.service.pool.PoolInfoProvider
import com.rarible.protocol.order.core.service.pool.PoolOwnershipService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

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
    private lateinit var orderValidator: OrderValidator

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
    private lateinit var orderSignatureResolver: OrderSignatureResolver

    @SpyK
    private var featureFlags: OrderIndexerProperties.FeatureFlags = OrderIndexerProperties.FeatureFlags()

    @Test
    fun `validate and get not found`() = runBlocking<Unit> {
        val order = createOrder()
        coEvery { orderRepository.findById(order.hash) } returns null

        assertThatExceptionOfType(EntityNotFoundApiException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }
    }

    @Test
    fun `validate and get not active`() = runBlocking<Unit> {
        val order = createOrder(
            end = Instant.now().minusSeconds(100).epochSecond
        )
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery { orderUpdateService.update(order.hash, any()) } returns Unit

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }.withMessage("order is not active")

        coVerify { orderUpdateService.update(order.hash, any()) }
    }

    @Test
    fun `validate and get no approval`() = runBlocking<Unit> {
        val order = createOrder()
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery {
            orderUpdateService.updateApproval(
                order = order,
                approved = false,
                eventTimeMarks = any()
            )
        } returns Unit
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns false
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns true

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }.withMessage("order is not approved")

        coVerify { orderUpdateService.updateApproval(order = order, approved = false, eventTimeMarks = any()) }
    }

    @Test
    fun `validate and get no erc20 allowance`() = runBlocking<Unit> {
        val order = createOrder()
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery {
            orderUpdateService.updateApproval(
                order = order,
                approved = false,
                eventTimeMarks = any()
            )
        } returns Unit
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns true
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns false

        assertThatExceptionOfType(ValidationApiException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }.withMessage("order is not approved")

        coVerify { orderUpdateService.updateApproval(order = order, approved = false, eventTimeMarks = any()) }
    }

    @Test
    fun `validate seaport`() = runBlocking<Unit> {
        val order = createOrder().copy(
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1()
        )
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery { orderSignatureResolver.resolveSeaportSignature(order.hash) } throws OrderDataException("wrong order data")
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns true
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns true

        assertThatExceptionOfType(OrderDataException::class.java).isThrownBy {
            runBlocking {
                orderService.validateAndGet(order.hash)
            }
        }.withMessage("wrong order data")
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = createOrder()
        coEvery { orderRepository.findById(order.hash) } returns order
        coEvery { approveService.checkOnChainApprove(order.maker, order.make.type, order.platform) } returns true
        coEvery { approveService.checkOnChainErc20Allowance(order.maker, order.make) } returns true

        assertThat(orderService.validateAndGet(order.hash)).isEqualTo(order)
    }
}