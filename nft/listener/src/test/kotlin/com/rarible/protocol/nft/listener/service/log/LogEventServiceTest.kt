package com.rarible.protocol.nft.listener.service.log

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.log.service.LogEventService
import com.rarible.protocol.contracts.erc1155.rarible.factory.Create1155RaribleProxyEvent
import com.rarible.protocol.contracts.erc1155.rarible.factory.user.Create1155RaribleUserProxyEvent
import com.rarible.protocol.contracts.erc721.rarible.factory.Create721RaribleProxyEvent
import com.rarible.protocol.contracts.erc721.rarible.factory.user.Create721RaribleUserProxyEvent
import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class LogEventServiceTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var service: LogEventService

    @Autowired
    private lateinit var descriptors: List<LogEventDescriptor<*>>

    @Test
    fun allPresentInMap() {
        assertThat(service.map.filter {
            !listOf(
                Create721RaribleProxyEvent.id(),
                Create721RaribleUserProxyEvent.id(),
                Create1155RaribleProxyEvent.id(),
                Create1155RaribleUserProxyEvent.id()
            ).contains(it.key)
        }).isEqualTo(descriptors.associate { it.topic to it.collection })
    }
}
