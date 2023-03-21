package com.rarible.protocol.erc20.listener.service.checker

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.listener.configuration.BalanceCheckerProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scalether.core.MonoEthereum
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

class BalanceCheckerHandler(
    private val sender: ReadOnlyMonoTransactionSender,
    private val ethereum: MonoEthereum,
    private val checkerMetrics: CheckerMetrics,
    private val props: BalanceCheckerProperties
) : ConsumerEventHandler<Erc20BalanceEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lastUpdated = Instant.MIN
    private var lastBlockNumber: Long = 0

    override suspend fun handle(event: Erc20BalanceEventDto) {
        checkerMetrics.onIncoming()
        if (event is Erc20BalanceUpdateEventDto) {
            val balance = event.balance
            val blockNumber = currentBlockNumber()
            val eventBlockNumber = balance.blockNumber
            if (eventBlockNumber != null && blockNumber - eventBlockNumber < props.skipNumberOfBlocks) {
                checkerMetrics.onCheck()
                val blockChainBalance = currentBalance(balance)
                if (balance.balance != blockChainBalance) {
                    checkerMetrics.onInvalid()
                    logger.error("Balance is invalid: [owner=${balance.owner} contract=${balance.contract} balance=${balance.balance}(${blockChainBalance}) block=${blockNumber}")
                }
            }
        }
    }

    private suspend fun currentBlockNumber(): Long {
        if (Duration.between(lastUpdated, Instant.now()) > props.updateLastBlock) {
            lastBlockNumber = ethereum.ethBlockNumber().awaitFirst().toLong()
        }
        return lastBlockNumber
    }

    private suspend fun currentBalance(balance: Erc20BalanceDto): BigInteger {
        //            val tx = Transaction(
//                balance.contract,
//                sender.from(),
//                BigInteger.ZERO,
//                BigInteger.ZERO,
//                BigInteger.ZERO,
//                Binary(ByteArray(1)),
//                null
//            )
//            val data = ethereum.ethCall(tx, "latest").awaitFirstOrNull()
//            println(data)
//            checkerMetrics.onCheck()
//            println("$blockNumber} - ${balance.balance}")
        return BigInteger.ONE
    }
}
