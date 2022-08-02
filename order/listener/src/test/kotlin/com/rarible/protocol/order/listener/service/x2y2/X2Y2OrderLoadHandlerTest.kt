package com.rarible.protocol.order.listener.service.x2y2

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.X2Y2FetchState
import com.rarible.protocol.order.core.repository.x2y2.X2Y2FetchStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2OrdersLoadWorkerProperties
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.x2y2.client.X2Y2ApiClient
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

@FlowPreview
@IntegrationTest
class X2Y2OrderLoadHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var stateRepository: X2Y2FetchStateRepository

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @MockkBean
    private lateinit var x2y2ApiClient: X2Y2ApiClient

    @Autowired
    private lateinit var converter: X2Y2OrderConverter

    private val saveCounter = mockk<RegisteredCounter> {
        every { increment() } just Runs
    }
    private val errorCounter = mockk<RegisteredCounter> {
        every { increment() } just Runs
    }

    private val orders = ClassPathResource("json/x2y2/orders.json").inputStream.use {
        mapper.readValue(it, object : TypeReference<ApiListResponse<Order>>() {})
    }

    private lateinit var handler: X2Y2OrderLoadHandler

    @BeforeEach
    fun setUpTest() {
        clearMocks(x2y2ApiClient)
        handler = X2Y2OrderLoadHandler(
            stateRepository,
            x2y2ApiClient,
            converter,
            orderRepository,
            saveCounter,
            errorCounter,
            orderUpdateService,
            X2Y2OrdersLoadWorkerProperties(
                enabled = true
            )
        )
    }

    @Test
    internal fun `should save new orders`() {
        runBlocking {
            coEvery {
                x2y2ApiClient.orders(cursor = any())
            } returns orders

            handler.handle()

            verify(exactly = 20) {
                saveCounter.increment()
            }

            verify(exactly = 0) {
                errorCounter.increment()
            }

            Wait.waitAssert {
                val expectedHashes = orders.data.map { it.itemHash }
                val saved = orderRepository.findAll(expectedHashes).toList()
                assertThat(saved.size).isEqualTo(20)
                val state = stateRepository.byId(X2Y2FetchState.ID)
                assertThat(state).isNotNull
                assertThat(state?.cursor).isNotNull
                assertThat(state?.cursor).isEqualTo("WzE2NDQxNTk0NTUwMDBd")
            }
        }
    }


    @Test
    internal fun `should increment error counter`() {
        runBlocking {
            coEvery {
                x2y2ApiClient.orders(cursor = any())
            } throws IllegalStateException("Something went wrong")

            assertThrows<IllegalStateException>(message = "Unable to load x2y2 orders! Something went wrong") {
                handler.handle()
            }

            verify(atLeast = 1) {
                errorCounter.increment()
            }

            val state = stateRepository.byId(X2Y2FetchState.ID)
            assertThat(state).isNotNull
            assertThat(state?.cursor).isNull()
            assertThat(state?.lastError).isNotNull
        }

    }
}

