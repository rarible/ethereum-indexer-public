package com.rarible.protocol.erc20.listener.data

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import io.daonomic.rpc.domain.Word

fun randomErc20ReduceEvent(event: Erc20TokenHistory): Erc20ReduceEvent {
    return Erc20ReduceEvent(
        logEvent = LogEvent(
            address = randomAddress(),
            blockHash = Word.apply(ByteArray(32)),
            data = event,
            blockNumber = 1L,
            index = 1,
            minorLogIndex = 1,
            status = LogEventStatus.CONFIRMED,
            topic = Word.apply(ByteArray(32)),
            transactionHash = Word.apply(ByteArray(32))
        ),
        blockNumber = 1L
    )
}