package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.EventData
import io.daonomic.rpc.domain.Word

interface OrderHistory : EventData {
    val hash: Word
}