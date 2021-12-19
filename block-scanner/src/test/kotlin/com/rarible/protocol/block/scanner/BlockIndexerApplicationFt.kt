package com.rarible.protocol.block.scanner

import com.rarible.protocol.block.scanner.test.AbstractIntegrationTest
import com.rarible.protocol.block.scanner.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@FlowPreview
@IntegrationTest
internal class BlockIndexerApplicationFt : AbstractIntegrationTest() {
    @Test
    fun `should scan block`() = runBlocking<Unit> {
        assertThat(ethereum).isNotNull
        assertThat(poller).isNotNull
    }
}
