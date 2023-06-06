package com.rarible.protocol.erc20.listener.data

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import io.daonomic.rpc.domain.Word

fun randomErc20ReduceEvent(event: Erc20TokenHistory): Erc20ReduceEvent {
    return Erc20ReduceEvent(
        logEvent = ReversedEthereumLogRecord(
            id = randomString(),
            address = randomAddress(),
            blockHash = Word.apply(ByteArray(32)),
            data = event,
            blockNumber = 1L,
            index = 1,
            minorLogIndex = 1,
            status = EthereumLogStatus.CONFIRMED,
            topic = Word.apply(ByteArray(32)),
            transactionHash = randomWord()
        ),
        blockNumber = 1L
    )
}