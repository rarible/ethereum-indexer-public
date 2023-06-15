package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.Platform
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderPlatformValidatorTest {
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()

    @Test
    fun `validate - ok, rarible`() = runBlocking<Unit> {
        val validator = OrderPlatformValidator(featureFlags)
        val version = createOrderVersion().copy(platform = Platform.RARIBLE)
        validator.validate(version)
    }

    @Test
    fun `validate - ok, cmp`() = runBlocking<Unit> {
        every { featureFlags.enableCmpOrders } returns true
        val validator = OrderPlatformValidator(featureFlags)
        val version = createOrderVersion().copy(platform = Platform.CMP)
        validator.validate(version)
    }

    @Test
    fun `validate - false, cmp`() = runBlocking<Unit> {
        every { featureFlags.enableCmpOrders } returns false
        val validator = OrderPlatformValidator(featureFlags)
        val version = createOrderVersion().copy(platform = Platform.CMP)

        assertThrows<ValidationApiException> {
            runBlocking {
                validator.validate(version)
            }
        }
    }
}