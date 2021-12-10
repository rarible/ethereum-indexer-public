package com.rarible.protocol.nft.core.service

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipReducer
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
class ReduceService(
    private val skipTokens: ReduceSkipTokens,
    private val itemReducer: ItemReducer,
    private val ownershipReducer: OwnershipReducer,
    private val historyRepository: NftItemHistoryRepository,
    private val lazyHistoryRepository: LazyNftItemHistoryRepository
)  {
    suspend fun onItemHistories(logs: List<LogEvent>): Mono<Void> {
        logger.info("onHistories ${logs.size} logs")
        logs
            .map { it.data as ItemHistory }
            .map { history -> ItemId(history.token, history.tokenId) }
            .filter { skipTokens.allowReducing(it.token, it.tokenId) }
            .distinct()
            .forEach { update(token = it.token, tokenId = it.tokenId) }
    }

    suspend fun update(token: Address? = null, tokenId: EthUInt256? = null, from: ItemId? = null): Flux<ItemId> {
        logger.info("Update token=$token, tokenId=$tokenId")
        Flux.mergeComparing(
            compareBy<HistoryLog>(
                { it.item.token.toString() },
                { it.item.tokenId },
                { it.log.blockNumber },
                { it.log.logIndex }
            ),
            findLazyItemsHistory(token, tokenId, from),
            historyRepository.findItemsHistory(token, tokenId, from)
        ).asFlow().collect { event ->

        }
    }

    private fun findLazyItemsHistory(token: Address?, tokenId: EthUInt256?, from: ItemId?): Flux<HistoryLog> {
        return lazyHistoryRepository.find(token, tokenId, from).map {
            HistoryLog(
                item = it,
                log = LogEvent(
                    data = it,
                    address = Address.ZERO(),
                    topic = Word.apply(ByteArray(32)),
                    transactionHash = Word.apply(ByteArray(32)),
                    status = LogEventStatus.CONFIRMED,
                    blockNumber = -1,
                    logIndex = -1,
                    index = 0,
                    minorLogIndex = 0
                )
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ReduceService::class.java)
    }
}
