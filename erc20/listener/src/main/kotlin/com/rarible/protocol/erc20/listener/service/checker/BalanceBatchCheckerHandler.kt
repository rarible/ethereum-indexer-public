package com.rarible.protocol.erc20.listener.service.checker

import com.rarible.contracts.erc20.IERC20
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import com.rarible.protocol.erc20.listener.configuration.BalanceCheckerProperties
import io.daonomic.rpc.domain.Request
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scala.Some
import scala.jdk.javaapi.CollectionConverters
import scalether.core.MonoEthereum
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.random.Random.Default.nextLong

class BalanceBatchCheckerHandler(
    private val balanceRepository: Erc20BalanceRepository,
    private val ethereum: MonoEthereum,
    private val checkerMetrics: CheckerMetrics,
    private val props: BalanceCheckerProperties
) : ConsumerBatchEventHandler<Erc20BalanceEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lastUpdated = Instant.MIN
    private var lastBlockNumber: Long = 0

    override suspend fun handle(events: List<Erc20BalanceEventDto>) {
        logger.info("Handling ${events.size} Erc20BalanceEventDto events")
        events.sortedByDescending { it.lastUpdatedAt }.distinctBy { it.balanceId }.forEach { handle(it) }
    }

    suspend fun handle(event: Erc20BalanceEventDto) {
        checkerMetrics.onIncoming()
        if (event is Erc20BalanceUpdateEventDto) {
            val balance = event.balance
            val blockNumber = currentBlockNumber()
            val eventBlockNumber = balance.blockNumber
            if (eventBlockNumber != null && blockNumber - eventBlockNumber < props.skipNumberOfBlocks) {
                checkerMetrics.onCheck()
                val blockChainBalance = getBalance(balance, eventBlockNumber)
                if (balance.balance != blockChainBalance && checkInDb(balance, blockChainBalance)) {
                    checkerMetrics.onInvalid()
                    logger.error("Balance is invalid: [owner=${balance.owner} contract=${balance.contract} balance=${balance.balance}(actual=${blockChainBalance}) block=${blockNumber}")
                }
            }
        }
    }

    private suspend fun checkInDb(event: Erc20BalanceDto, prevBalance: BigInteger): Boolean {
        val balanceId = BalanceId(event.contract, event.owner)
        val dbBalance = balanceRepository.get(balanceId)
        logger.info("Fallback to getting balance from DB for checking $balanceId")
        return if (dbBalance?.blockNumber != null) {
            val blockchainBalance = if (dbBalance?.blockNumber != event.blockNumber) {
                getBalance(event, dbBalance.blockNumber!!)
            } else prevBalance
            val result = blockchainBalance != dbBalance.balance.value
            if (!result) {
                logger.error("Balance in db is invalid: [id=$balanceId balance=${dbBalance.balance.value}(actual=$blockchainBalance) block=${dbBalance.blockNumber}")
            }
            result
        } else true
    }

    // to prevent additional requests to the node we cache current block for some time
    private suspend fun currentBlockNumber(): Long {
        val now = Instant.now()
        if (Duration.between(lastUpdated, now) > props.updateLastBlock) {
            lastBlockNumber = ethereum.ethBlockNumber().awaitFirst().toLong()
            lastUpdated = now
        }
        return lastBlockNumber
    }

    private suspend fun getBalance(balance: Erc20BalanceDto, blockNumber: Long): BigInteger {
        val hexBlock = "0x%x".format(blockNumber)
        val data = IERC20.balanceOfSignature().encode(balance.owner)
        val seq = CollectionConverters.asScala(listOf(mapOf(
            "data" to data.prefixed(),
            "to" to balance.contract.prefixed()
        ), hexBlock)).toSeq()
        val request = Request.apply(nextLong(), "eth_call", seq)
        val response = ethereum.executeRaw(request).awaitFirst()
        val textValue = (response.result() as Some).value().textValue()
        return EthUInt256.of(textValue).value
    }

}
