package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.composit.CompositeFullReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address

@Component
@ExperimentalCoroutinesApi
class ItemReduceServiceV2(
    private val skipTokens: ReduceSkipTokens,
    private val compositeFullReduceService: CompositeFullReduceService,
    private val historyRepository: NftItemHistoryRepository,
    private val lazyHistoryRepository: LazyNftItemHistoryRepository,
    private val ownershipEventConverter: OwnershipEventConverter
) : ItemReduceService {

    override fun onItemHistories(logs: List<LogEvent>): Mono<Void> {
        if (logs.isNotEmpty()) {
            logger.info("onHistories ${logs.size} logs")
        }
        return logs.toFlux()
            .map { it.data as ItemHistory }
            .filter { skipTokens.allowReducing(it.token, it.tokenId) }
            .flatMap { Flux.just(ItemId(it.token, it.tokenId)) }
            .distinct()
            .flatMap { update(token = it.token, tokenId = it.tokenId, from = null) }
            .then()
    }

    override fun update(token: Address?, tokenId: EthUInt256?, from: ItemId?): Flux<ItemId> = flux {
        logger.info("Update token=$token, tokenId=$tokenId")
        val events = Flux.mergeComparing(
            compareBy<HistoryLog>(
                { it.item.token.toString() },
                { it.item.tokenId },
                { it.log.blockNumber },
                { it.log.logIndex }
            ),
            findLazyItemsHistory(token, tokenId, from),
            historyRepository.findItemsHistory(token, tokenId, from)
        ).concatMap {
            mono {
                CompositeEvent(
                    itemEvent = ItemEventConverter.convert(it.log),
                    ownershipEvents = ownershipEventConverter.convert(it.log)
                )
            }
        }.filter {
            it.itemEvent != null || it.ownershipEvents.isNotEmpty()
        }
        compositeFullReduceService.reduce(events.asFlow()).collect { entity ->
            send(entity.id)
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
                    createdAt = it.date,
                    updatedAt = it.date,
                    blockNumber = -1,
                    logIndex = -1,
                    index = 0,
                    minorLogIndex = 0
                )
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ItemReduceServiceV2::class.java)
    }
}
