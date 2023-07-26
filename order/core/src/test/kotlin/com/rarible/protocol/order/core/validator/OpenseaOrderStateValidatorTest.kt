package com.rarible.protocol.order.core.validator

import com.rarible.core.test.data.randomBinary
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.exception.OrderDataException
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.SeaportSignatureResolver
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpenseaOrderStateValidatorTest {
    @InjectMockKs
    private lateinit var openseaOrderStateValidator: OpenseaOrderStateValidator

    @MockK
    private lateinit var seaportSignatureResolver: SeaportSignatureResolver

    @Test
    fun `validate seaport`() = runBlocking<Unit> {
        val order = randomOrder().copy(
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1()
        )
        coEvery { seaportSignatureResolver.resolveSeaportSignature(order.hash) } throws OrderDataException("wrong order data")

        assertThatExceptionOfType(OrderDataException::class.java).isThrownBy {
            runBlocking {
                openseaOrderStateValidator.validate(order)
            }
        }.withMessage("wrong order data")
    }

    @Test
    fun `supports - false`() = runBlocking<Unit> {
        val order = randomOrder()

        assertThat(openseaOrderStateValidator.supportsValidation(order)).isFalse()
    }

    @Test
    fun `validate valid`() = runBlocking<Unit> {
        val order = randomOrder().copy(
            platform = Platform.OPEN_SEA,
            data = createOrderBasicSeaportDataV1()
        )
        coEvery { seaportSignatureResolver.resolveSeaportSignature(order.hash) } returns randomBinary()

        openseaOrderStateValidator.validate(order)
    }
}
