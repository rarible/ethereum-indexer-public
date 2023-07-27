package com.rarible.protocol.order.listener.misc

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumLogEventSubscriber
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

suspend inline fun <reified T> EthereumLogEventSubscriber.convert(
    log: Log,
    transaction: Transaction,
    timestamp: Long,
    index: Int,
    totalLogs: Int
): List<T> {
    val ethBlock = EthereumBlockchainBlock(
        number = 1,
        hash = randomWord(),
        parentHash = randomWord(),
        timestamp = timestamp,
        ethBlock = mockk()
    )
    val ethLog = EthereumBlockchainLog(
        ethLog = log,
        ethTransaction = transaction,
        index = index,
        total = totalLogs,
    )
    return getEthereumEventRecords(ethBlock, ethLog)
        .map { it.asEthereumLogRecord().data as T }
}

suspend inline fun <reified T> EthereumLogEventSubscriber.convert(
    log: Log,
    timestamp: Long,
    index: Int,
    totalLogs: Int
): List<T> {
    return convert(log, createTransactionMockk(), timestamp, index, totalLogs)
}

suspend inline fun <reified T> EthereumLogEventSubscriber.convert(
    log: Log,
    transactionInput: Binary,
    timestamp: Long,
    index: Int,
    totalLogs: Int
): List<T> {
    val transaction = createTransactionMockk()
    every { transaction.input() } returns transactionInput
    return convert(log, transaction, timestamp, index, totalLogs)
}

suspend inline fun <reified T> EthereumLogEventSubscriber.convert(
    log: Log,
    transaction: Transaction,
    timestamp: Instant,
    index: Int,
    totalLogs: Int
): List<T> = convert(log, transaction, timestamp.epochSecond, index, totalLogs)

fun createTransactionMockk(): Transaction {
    return mockk<Transaction> {
        every { input() } returns Binary.empty()
        every { hash() } returns Word.apply(randomWord())
        every { from() } returns randomAddress()
        every { to() } returns randomAddress()
    }
}
