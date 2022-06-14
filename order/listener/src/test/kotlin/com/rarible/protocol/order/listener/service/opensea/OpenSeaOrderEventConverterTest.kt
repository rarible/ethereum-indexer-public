package com.rarible.protocol.order.listener.service.opensea


import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter.Companion.IGNORE_LIST
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class OpenSeaOrderEventConverterTest() : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var openSeaOrderEventConverter: OpenSeaOrderEventConverter

    @Test
    fun `should throw unsupported exception`() = runBlocking<Unit> {
        assertThrows<UnsupportedOperationException> {
            openSeaOrderEventConverter.encodeTransfer(Binary.apply("0x123456"))
        }
    }

    @Test
    fun `should return null`() = runBlocking<Unit> {
        val result = openSeaOrderEventConverter.encodeTransfer(IGNORE_LIST[0])
        assertThat(result).isNull()
    }

}