package com.rarible.protocol.nft.core.service.item

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.HistoryLog
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ReduceSkipTokens
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.service.composit.SilentCompositeFullReduceService
import com.rarible.protocol.nft.core.service.composit.VerboseCompositeFullReduceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address

@Service
class ItemReduceService(
    private val skipTokens: ReduceSkipTokens,
    private val silentFullReduceService: SilentCompositeFullReduceService,
    private val verboseFullReduceService: VerboseCompositeFullReduceService,
    private val historyRepository: NftItemHistoryRepository,
    private val lazyHistoryRepository: LazyNftItemHistoryRepository,
    private val ownershipEventConverter: OwnershipEventConverter,
    private val itemEventConverter: ItemEventConverter
) {

    fun onItemHistories(logs: List<LogEvent>): Mono<Void> {
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

    fun update(
        token: Address? = null,
        tokenId: EthUInt256? = null,
        from: ItemId? = null,
        to: ItemId? = null,
        updateNotChanged: Boolean = true
    ): Flux<ItemId> = flux {
        logger.info("Update token=$token, tokenId=$tokenId from=$from to=$to")
        val eventTimeMarks = nftOffchainEventMarks()
        val events = Flux.mergeComparing(
            compareBy<HistoryLog>(
                { it.item.token.toString() },
                { it.item.tokenId },
                { it.log.blockNumber },
                { it.log.logIndex }
            ),
            findLazyItemsHistory(token, tokenId, from, to),
            historyRepository.findItemsHistory(token, tokenId, from, to, listOf(LogEventStatus.CONFIRMED))
        ).concatMap {
            logger.info("Item reduce HistoryLog=$it")
            mono {
                CompositeEvent(
                    itemEvent = itemEventConverter.convert(it.log, eventTimeMarks),
                    ownershipEvents = ownershipEventConverter.convert(it.log, eventTimeMarks)
                )
            }
        }.filter {
            it.itemEvent != null || it.ownershipEvents.isNotEmpty()
        }.onErrorContinue { ex, event ->
            logger.error("Cause of error is $event", ex)
        }

        val fullReduceService = if (updateNotChanged) verboseFullReduceService else silentFullReduceService

        fullReduceService.reduce(events.asFlow()).collect { entity ->
            send(entity.id)
        }
    }

    private fun findLazyItemsHistory(
        token: Address?,
        tokenId: EthUInt256?,
        from: ItemId?,
        to: ItemId?
    ): Flux<HistoryLog> {
        return lazyHistoryRepository.find(token, tokenId, from, to).map {
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

    companion object {

        val WORD_ZERO: Word = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000")
        private val logger = LoggerFactory.getLogger(ItemReduceService::class.java)
    }
}
