package com.rarible.protocol.order.core.service.block.handler

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.service.pool.listener.PoolOrderEventListener
import org.springframework.stereotype.Component

@Component
class PoolEthereumEventHandler(
    private val poolOrderEventListener: PoolOrderEventListener,
    properties: OrderIndexerProperties.PoolEventHandleProperties
) : AbstractEthereumEventHandler<LogEvent>(properties, { poolOrderEventListener.onPoolEvent(it) })