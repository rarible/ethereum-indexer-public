package com.rarible.protocol.order.listener.service.sudoswap

import com.rarible.contracts.erc721.TransferEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SpotPriceUpdateEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
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
    private val ethereum: MonoEthereum
) {
    suspend fun detectNftTransfers(
        sudoSwapNftOutPairLog: Log,
        nftCollection: Address
    ): List<BigInteger> {
        val poolAddress = sudoSwapNftOutPairLog.address()
        val blockHash = sudoSwapNftOutPairLog.blockHash()
        val transactionHash = sudoSwapNftOutPairLog.transactionHash()

        val filter = LogFilter
            .apply(TopicFilter.or(SpotPriceUpdateEvent.id(), TransferEvent.id(), SwapNFTOutPairEvent.id()))
            .address(sudoSwapNftOutPairLog.address(), nftCollection)
            .blockHash(blockHash)

        val logs = try {
            ethereum.ethGetLogsJava(filter).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Unable to get logs for block $blockHash", e)
            throw e
        }
        fun isTargetLog(log: Log, topic: Word, address: Address): Boolean {
            return log.topics().head() == topic &&
                   log.address() == address &&
                   log.transactionHash() == transactionHash
        }
        val tokenIds = mutableListOf<BigInteger>()
        var foundTargertNftOutLog = false

        val iterator = logs.listIterator(logs.size)
        while (iterator.hasPrevious()) {
            val log = iterator.previous()

            foundTargertNftOutLog = foundTargertNftOutLog ||
                isTargetLog(log, SwapNFTOutPairEvent.id(), poolAddress) &&
                log.logIndex() == sudoSwapNftOutPairLog.logIndex()

            if (foundTargertNftOutLog && isTargetLog(log, TransferEvent.id(), nftCollection)) {
                val transfer = TransferEvent.apply(log)
                if (transfer.from() == poolAddress) {
                    tokenIds.add(transfer.tokenId())
                }
            }
            if (foundTargertNftOutLog && isTargetLog(log, SpotPriceUpdateEvent.id(), poolAddress)) {
                break
            }
        }
        return tokenIds
    }


    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SudoSwapNftTransferDetector::class.java)
    }
}