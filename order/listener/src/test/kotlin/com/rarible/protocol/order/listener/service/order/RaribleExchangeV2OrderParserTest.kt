package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
internal class RaribleExchangeV2OrderParserTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    @Autowired
    private lateinit var raribleExchangeV2OrderParser: RaribleExchangeV2OrderParser

    @Test
    fun `should decode order match transaction input`() {
    }
}
