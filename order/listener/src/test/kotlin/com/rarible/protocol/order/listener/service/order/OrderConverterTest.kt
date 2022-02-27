package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderConverter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlinx.coroutines.FlowPreview

@IntegrationTest
@FlowPreview
class OrderConverterTest {
    @Autowired
    private lateinit var orderConverter: OpenSeaOrderConverter

    @Test
    fun test() {
       // val order = createOrderOpenSeaV1DataV1()
        orderConverter.testIncrement()
    }

}