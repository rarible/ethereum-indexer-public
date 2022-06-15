package com.rarible.protocol.order.listener.service.log

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class LogEventServiceTest: AbstractIntegrationTest() {
    @Autowired
    private lateinit var service: LogEventService
    @Autowired
    private lateinit var descriptors: List<LogEventDescriptor<*>>

    @Test
    fun allPresentInMap() {
        assertThat(service.map)
            .isEqualTo(descriptors.associate { it.topic to it.collection })
    }
}
