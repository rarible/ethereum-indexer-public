package com.rarible.protocol.nft.listener.service.checker

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.metric.CheckerMetrics
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import scala.Some
import scala.Tuple2
import scala.jdk.javaapi.CollectionConverters
import scalether.core.MonoEthereum
import scalether.domain.Address
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class OwnershipBatchCheckerHandler(
    private val nftListenerProperties: NftListenerProperties,
    private val ethereum: MonoEthereum,
    private val tokenRepository: TokenRepository,
    private val checkerMetrics: CheckerMetrics
) : ConsumerBatchEventHandler<NftOwnershipEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lastUpdated = Instant.MIN
    private var lastBlockNumber: Long = 0

    // Map<Block number, Map<ownershipId, balance>>
    private val blockBuffer = emptyMap<Long, MutableMap<OwnershipId, BigInteger>>().toSortedMap()
    private val props = nftListenerProperties.ownershipCheckerProperties

    override suspend fun handle(events: List<NftOwnershipEventDto>) {
        logger.info("Handling ${events.size} NftOwnershipEventDto events")
        try {
            events.forEach { fillBuffer(it) }
            checkBuffer()
            consumeBuffer()
        } catch (ex: Exception) {
            logger.error("Error during checking ownership", ex)
        }
    }

    private suspend fun fillBuffer(event: NftOwnershipEventDto) {
        checkerMetrics.onIncoming()
        val blockNumber = currentBlockNumber()
        val eventBlockNumber = event.blockNumber
        if (eventBlockNumber != null && blockNumber - eventBlockNumber < props.skipNumberOfBlocks) {
            val eventBuffer: MutableMap<OwnershipId, BigInteger> =
                blockBuffer.getOrPut(eventBlockNumber) { mapOf<OwnershipId, BigInteger>().toMutableMap() }
            when (event) {
                is NftOwnershipUpdateEventDto -> eventBuffer[OwnershipId.parseId(event.ownershipId)] = event.ownership.value
                is NftOwnershipDeleteEventDto -> eventBuffer[OwnershipId.parseId(event.ownershipId)] = BigInteger.ZERO
            }
        }
    }

    private suspend fun checkBuffer() {
        val blockNumber = currentBlockNumber()
        blockBuffer.entries.removeIf {
            val isDeleted = blockNumber - it.key >= props.skipNumberOfBlocks
            if (isDeleted) {
                checkerMetrics.onSkipped()
                logger.info("Events for ${it.key} are outdated")
            }
            isDeleted
        }
    }

    private suspend fun consumeBuffer() {
        blockBuffer.asIterable().take(maxOf(blockBuffer.size - props.confirms, 0)).forEach {
            val eventBlockNumber = it.key
            val events = blockBuffer.remove(eventBlockNumber)
            logger.info("Start process ${events?.size} ownerships from $eventBlockNumber block [${blockBuffer.size} cached block]")
            events?.forEach { (ownershipId, value) ->
                val blockChainBalance = valueOfToken(ownershipId, eventBlockNumber)
                if (blockChainBalance != null) {
                    if (value != blockChainBalance) {
                        checkerMetrics.onFail()
                        logger.error("Ownership is invalid: [id=$ownershipId value=$value(actual=$blockChainBalance) block=$eventBlockNumber")
                    } else {
                        checkerMetrics.onSuccess()
                    }
                } else {
                    checkerMetrics.onSkipped()
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

    private suspend fun valueOfToken(ownershipId: OwnershipId, blockNumber: Long): BigInteger? {
        val token = tokenRepository.findById(ownershipId.token).awaitFirstOrNull()
        return when (token?.standard) {
            TokenStandard.ERC721 -> valueOf721Token(ownershipId, blockNumber)
            TokenStandard.ERC1155 -> valueOf1151Token(ownershipId, blockNumber)
            else -> {
                logger.warn("Ignoring checking token ownership due to token standard is missed or token doesn't exist: $ownershipId")
                null
            }
        }
    }

    private suspend fun valueOf721Token(ownershipId: OwnershipId, blockNumber: Long): BigInteger {
        val data = IERC721.ownerOfSignature().encode(ownershipId.tokenId.value)
        val owner = Address.apply("0x" + makeRawRequest(data, ownershipId, blockNumber).substring(26))
        return when (owner) {
            ownershipId.owner -> BigInteger.ONE
            else -> BigInteger.ZERO
        }
    }

    private suspend fun valueOf1151Token(ownershipId: OwnershipId, blockNumber: Long): BigInteger {
        val data = IERC1155.balanceOfSignature().encode(Tuple2.apply(ownershipId.owner, ownershipId.tokenId.value))
        val response = makeRawRequest(data, ownershipId, blockNumber)
        return EthUInt256.of(response).value
    }

    private suspend fun makeRawRequest(data: Binary, ownershipId: OwnershipId, blockNumber: Long): String {
        val hexBlock = "0x%x".format(blockNumber)
        val seq = CollectionConverters.asScala(
            listOf(
                mapOf(
                    "data" to data.prefixed(),
                    "to" to ownershipId.token.prefixed()
                ), hexBlock
            )
        ).toSeq()
        val request = Request.apply(Random.nextLong(), "eth_call", seq)
        val response = ethereum.executeRaw(request).awaitFirst()
        return (response.result() as Some).value().textValue()
    }
}
