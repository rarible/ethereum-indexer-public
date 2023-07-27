package com.rarible.protocol.order.listener

import com.rarible.protocol.order.listener.integration.TestPropertiesConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ContextConfiguration(classes = [TestPropertiesConfiguration::class])
@ExtendWith(SpringExtension::class)
class ContextTest {
    @Test
    fun `should start the context`() {
        // Empty test; we just want the context to load
    }
}
