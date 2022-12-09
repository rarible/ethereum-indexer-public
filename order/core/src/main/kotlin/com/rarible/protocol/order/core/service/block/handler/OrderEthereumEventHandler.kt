package com.rarible.protocol.order.core.service.block.handler

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component

@Component
class OrderEthereumEventHandler(
    private val orderUpdateService: OrderUpdateService,
    properties: OrderIndexerProperties.OrderEventHandleProperties
) : AbstractEthereumEventHandler<Word>(properties, { orderUpdateService.update(it) })

