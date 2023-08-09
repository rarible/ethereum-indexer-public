package com.rarible.protocol.erc20.listener.scanner

import com.rarible.contracts.erc20.IERC20
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.Erc20BalanceEventDto
import com.rarible.protocol.dto.Erc20BalanceUpdateEventDto
import com.rarible.protocol.erc20.core.metric.CheckerMetrics
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import io.daonomic.rpc.domain.Request
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scala.Some
import scala.jdk.javaapi.CollectionConverters
import scalether.core.MonoEthereum
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.random.Random.Default.nextLong

class BalanceBatchCheckerHandler(
    private val ethereum: MonoEthereum,
    private val checkerMetrics: CheckerMetrics,
    commonProps: Erc20ListenerProperties
) : RaribleKafkaEventHandler<Erc20BalanceEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lastUpdated = Instant.MIN
    private var lastBlockNumber: Long = 0
    private val props = commonProps.balanceCheckerProperties
    private val checkedContracts = commonProps.tokens.map { Address.apply(it) }.toSet()

    // Map<Block number, balance events>
    private val blockBuffer = emptyMap<Long, BufferMap>().toSortedMap()

    override suspend fun handle(event: Erc20BalanceEventDto) {
        if (props.enabled) {
            try {
                fillBuffer(event)
                checkBuffer()
                consumeBuffer()
            } catch (ex: Exception) {
                logger.error("Error during checking erc20 balances", ex)
            }
        }
    }

    private suspend fun fillBuffer(event: Erc20BalanceEventDto) {
        checkerMetrics.onIncoming()
        if (event is Erc20BalanceUpdateEventDto && (checkedContracts.isEmpty() || checkedContracts.contains(event.balance.contract))) {
            val balance = event.balance
            val blockNumber = currentBlockNumber()
            val eventBlockNumber = balance.blockNumber
            if (eventBlockNumber != null && blockNumber - eventBlockNumber < props.skipNumberOfBlocks) {
                val eventBuffer = blockBuffer.getOrPut(eventBlockNumber) { BufferMap() }
                eventBuffer.put(
                    key = event.balanceId,
                    value = ShortBalance(event.lastUpdatedAt ?: Instant.now(), event.balance.balance),
                    checkUpdatedAt = props.checkUpdatedAt
                )
            }
        }
    }

    private suspend fun checkBuffer() {
        val blockNumber = currentBlockNumber()
        blockBuffer.entries.removeIf {
            val isDeleted = blockNumber - it.key >= props.skipNumberOfBlocks
            if (isDeleted) {
                logger.info("Events for ${it.key} are outdated")
            }
            isDeleted
        }
    }

    private suspend fun consumeBuffer() {
        blockBuffer.asIterable().take(maxOf(blockBuffer.size - props.confirms, 0)).forEach {
            val eventBlockNumber = it.key
            val events = blockBuffer.remove(eventBlockNumber)
            logger.info("Start process ${events?.size} balances from $eventBlockNumber block [${blockBuffer.size} cached block]")
            events?.forEach { (t, u) ->
                val balanceId = BalanceId.parseId(t)
                val blockChainBalance = getBalance(balanceId.token, balanceId.owner, eventBlockNumber)
                checkerMetrics.onCheck()
                if (u.value != blockChainBalance) {
                    checkerMetrics.onInvalid()
                    logger.error("Balance is invalid: [id=$balanceId balance=${u.value}(actual=$blockChainBalance) block=$eventBlockNumber")
                }
            }
        }
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

    private suspend fun getBalance(contract: Address, owner: Address, blockNumber: Long): BigInteger {
        val hexBlock = "0x%x".format(blockNumber)
        val data = IERC20.balanceOfSignature().encode(owner)
        val seq = CollectionConverters.asScala(
            listOf(
                mapOf(
                    "data" to data.prefixed(),
                    "to" to contract.prefixed()
                ), hexBlock
            )
        ).toSeq()
        val request = Request.apply(nextLong(), "eth_call", seq)
        val response = ethereum.executeRaw(request).awaitFirst()
        val textValue = (response.result() as Some).value().textValue()
        return EthUInt256.of(textValue).value
    }

    // Map<BalanceId, Balance events>
    // We keep only significant fields for balances in order to decrease memory footprint
    class BufferMap : HashMap<String, ShortBalance>() {
        fun put(key: String, value: ShortBalance, checkUpdatedAt: Boolean): ShortBalance? {
            val existed = get(key)
            return if (existed == null || !checkUpdatedAt || existed.updated.isBefore(value.updated)) {
                super.put(key, value)
            } else {
                existed
            }
        }
    }

    data class ShortBalance(

        // date of event
        val updated: Instant,

        // balance
        val value: BigInteger
    )
}
