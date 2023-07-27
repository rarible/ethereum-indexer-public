package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SpotPriceUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.request.LogFilter
import scalether.domain.request.TopicFilter
import scalether.domain.response.Log
import java.math.BigInteger

@Component
class SudoSwapNftTransferDetector(
    private val ethereum: MonoEthereum,
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {
    suspend fun detectNftTransfers(
        sudoSwapNftOutPairLog: Log,
        nftCollection: Address
    ): List<BigInteger> {
        val poolAddress = sudoSwapNftOutPairLog.address()
        val transactionHash = sudoSwapNftOutPairLog.transactionHash()
        val collectionTransfers = getLogs(sudoSwapNftOutPairLog, nftCollection)

        val tokenIds = mutableListOf<BigInteger>()
        var foundTargertNftOutLog = false

        val iterator = collectionTransfers.logs.listIterator(collectionTransfers.logs.size)
        while (iterator.hasPrevious()) {
            val log = iterator.previous()

            foundTargertNftOutLog = foundTargertNftOutLog ||
                isTargetLog(log, SwapNFTOutPairEvent.id(), poolAddress, transactionHash) &&
                log.logIndex() == sudoSwapNftOutPairLog.logIndex()

            if (foundTargertNftOutLog && isTargetLog(log, collectionTransfers.topic, nftCollection, transactionHash)) {
                val transfer = collectionTransfers.parseTransfer(log)
                if (transfer.from == poolAddress) {
                    tokenIds.add(transfer.tokenId)
                }
            }
            if (foundTargertNftOutLog && isTargetLog(log, SpotPriceUpdateEvent.id(), poolAddress, transactionHash)) {
                break
            }
        }
        return tokenIds
    }

    private suspend fun getLogs(
        poolLog: Log,
        collection: Address
    ): CollectionTransfers {
        val pool = poolLog.address()
        val blockHash = poolLog.blockHash()
        val erc721Logs = getErc721Logs(blockHash, pool, collection)
        return if (erc721Logs.isEmpty() && featureFlags.searchSudoSwapErc1155Transfer) {
            getErc1155Logs(blockHash, pool, collection)
        } else {
            erc721Logs
        }
    }

    private suspend fun getErc721Logs(
        blockHash: Word,
        pool: Address,
        collection: Address,
    ): CollectionTransfers {
        val logs = getLogs(blockHash, pool, collection, Erc721CollectionTransfers.TOPIC)
        return Erc721CollectionTransfers(logs)
    }

    private suspend fun getErc1155Logs(
        blockHash: Word,
        pool: Address,
        collection: Address,
    ): CollectionTransfers {
        val logs = getLogs(blockHash, pool, collection, Erc1155CollectionTransfers.TOPIC)
        return Erc1155CollectionTransfers(logs)
    }

    private suspend fun getLogs(
        blockHash: Word,
        pool: Address,
        collection: Address,
        collectionTopic: Word
    ): List<Log> {
        val filter = LogFilter
            .apply(TopicFilter.or(SpotPriceUpdateEvent.id(), collectionTopic, SwapNFTOutPairEvent.id()))
            .address(pool, collection)
            .blockHash(blockHash)

        return try {
            ethereum.ethGetLogsJava(filter).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Unable to get logs for block $blockHash", e)
            throw e
        }
    }

    fun isTargetLog(log: Log, topic: Word, address: Address, transactionHash: Word): Boolean {
        return log.topics().head() == topic &&
                log.address() == address &&
                log.transactionHash() == transactionHash
    }

    private sealed class CollectionTransfers {
        abstract val logs: List<Log>
        abstract val topic: Word
        abstract fun parseTransfer(log: Log): Transfer

        fun isEmpty(): Boolean {
            return logs.none { topic in it.topics() }
        }
    }

    private class Erc721CollectionTransfers(
        override val logs: List<Log>
    ) : CollectionTransfers() {
        override val topic: Word = TOPIC

        override fun parseTransfer(log: Log): Transfer {
            val event = TransferEvent.apply(log)
            return Transfer(event.from(), event.tokenId())
        }

        companion object {
            val TOPIC: Word = TransferEvent.id()
        }
    }

    private class Erc1155CollectionTransfers(
        override val logs: List<Log>
    ) : CollectionTransfers() {
        override val topic: Word = TOPIC

        override fun parseTransfer(log: Log): Transfer {
            val event = TransferSingleEvent.apply(log)
            return Transfer(event._from(), event._id())
        }

        companion object {
            val TOPIC: Word = TransferSingleEvent.id()
        }
    }

    private data class Transfer(val from: Address, val tokenId: BigInteger)

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SudoSwapNftTransferDetector::class.java)
    }
}
