package com.rarible.protocol.nft.listener.service.item

import com.rarible.protocol.nft.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.listener.integration.IntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ItemAndOwnershipsCheckTaskHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var job: ItemAndOwnershipsCheckTaskHandler

    @Test
    fun
}