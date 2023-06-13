package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.order.api.data.createOrder
import com.rarible.protocol.order.api.exceptions.OrderDataException
import com.rarible.protocol.order.api.service.order.signature.OrderSignatureResolver
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.Platform
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpenseaOrderStateValidatorTest {
    @InjectMockKs
    private lateinit var openseaOrderStateValidator: OpenseaOrderStateValidator

    @MockK
    private lateinit var orderSignatureResolver: OrderSignatureResolver

    @Test
    fun `validate seaport`() = runBlocking<Unit> {
        val order = createOrder().copy(
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1()
        )
        coEvery { orderSignatureResolver.resolveSeaportSignature(order.hash) } throws OrderDataException("wrong order data")

        assertThatExceptionOfType(OrderDataException::class.java).isThrownBy {
            runBlocking {
                openseaOrderStateValidator.validate(order)
            }
        }.withMessage("wrong order data")
    }

    @Test
    fun `validate ignored`() = runBlocking<Unit> {
        val order = createOrder()

        openseaOrderStateValidator.validate(order)
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = createOrder().copy(
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1()
        )
        coEvery { orderSignatureResolver.resolveSeaportSignature(order.hash) } returns randomBinary()

        openseaOrderStateValidator.validate(order)
    }
}