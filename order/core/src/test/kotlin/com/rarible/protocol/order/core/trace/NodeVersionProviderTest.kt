package com.rarible.protocol.order.core.trace

import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.NodeType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@IntegrationTest
internal class NodeVersionProviderTest : AbstractIntegrationTest() {
    private lateinit var nodeVersionProvider: NodeVersionProvider

    @BeforeEach
    fun setup() {
        nodeVersionProvider = NodeVersionProvider(ethereum)
    }

    @Test
    fun getNodeClientVersion() = runBlocking<Unit> {
        val version = nodeVersionProvider.getClientVersion()
        assertThat(version?.type).isEqualTo(NodeType.OPEN_ETHEREUM)
    }
}
