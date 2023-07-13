package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderX2Y2DataV1
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomEth
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiOrderSignErrorResponse
import com.rarible.x2y2.client.model.OrderError
import com.rarible.x2y2.client.model.OrderSignResult
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class OrderX2Y2StatusTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var handler: OrderX2Y2StatusTaskHandler

    @Autowired
    private lateinit var properties: OrderListenerProperties

    @Autowired
    private lateinit var x2y2ApiClient: X2Y2ApiClient

    @Test
    internal fun `should cancel x2y2 orders status if signature was not fetched`() = runBlocking<Unit> {
        val updated = prepareX2Y2OrderAndRunTask(
            OrderSignResult.fail(
                ApiOrderSignErrorResponse(false, listOf(OrderError(2020, randomBigInt())))
            )
        )
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    internal fun `should not cancel x2y2 orders status if signature was fetched`() = runBlocking<Unit> {
        val updated = prepareX2Y2OrderAndRunTask(
            OrderSignResult.success(mockk())
        )
        assertThat(updated.status).isNotEqualTo(OrderStatus.CANCELLED)
    }


    private suspend fun prepareX2Y2OrderAndRunTask(apiResult: OrderSignResult): Order {
        properties.fixX2Y2 = true
        val taskParam = AbstractOrderUpdateStatusTaskHandler.TaskParam(
            status = OrderStatus.ACTIVE,
            platform = Platform.X2Y2,
            listedAfter = (Instant.now() - Duration.ofNanos(1)).epochSecond
        )
        val param = AbstractOrderUpdateStatusTaskHandler.objectMapper.writeValueAsString(taskParam)
        val hash = Word.apply(randomWord())
        val token = randomAddress()
        val tokenId = EthUInt256.of(randomBigInt())
        val assetNft = randomErc721(token)
        val data = createOrderX2Y2DataV1()
        val currencyAsset = randomEth()
        val orderVersion = createOrderVersion().copy(
            hash = hash,
            make = randomErc721(token, tokenId),
            take = currencyAsset,
            data = data,
            type = OrderType.X2Y2,
            platform = Platform.X2Y2
        )
        orderVersionRepository.save(orderVersion).awaitSingle()
        orderUpdateService.update(hash, orderStubEventMarks())

        val updated = orderRepository.findById(hash)
        Assertions.assertThat(updated?.status).isNotEqualTo(OrderStatus.CANCELLED)

        coEvery {
            x2y2ApiClient.fetchOrderSign(
                caller = orderVersion.maker.prefixed(),
                op = BigInteger.ONE,
                orderId = data.orderId,
                currency = Address.ZERO(),
                price = currencyAsset.value.value,
                tokenId = tokenId.value
            )
        } returns apiResult

        handler.runLongTask(null, param).toList()
        return orderRepository.findById(hash) ?: error("Can't fetch order")
    }
}
