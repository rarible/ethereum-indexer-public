package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.randomNftItemMeta
import org.junit.jupiter.api.BeforeEach

abstract class AbstractEventHandlerIt : AbstractIntegrationTest() {
    @BeforeEach
    fun mockMeta() {
        nftItemControllerApiMock.mockGetItemMetaById(null, randomNftItemMeta())
    }
}
