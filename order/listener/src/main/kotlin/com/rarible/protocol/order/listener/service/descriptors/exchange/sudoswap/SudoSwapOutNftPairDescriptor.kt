package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolAnyNftOut
import com.rarible.protocol.order.core.model.PoolExchangeHistory
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.model.SudoSwapAnyOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapTargetOutNftDetail
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import java.time.Instant
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
@CaptureSpan(type = SpanType.EVENT)
class SudoSwapOutNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter
): LogEventDescriptor<PoolExchangeHistory> {

    override val collection: String = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = SwapNFTOutPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolExchangeHistory> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): PoolExchangeHistory {
        val details = sudoSwapEventConverter.getSwapOutNftDetails(transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        return when (details) {
            is SudoSwapAnyOutNftDetail -> {
                PoolAnyNftOut(
                    hash = hash,
                    numberNft = EthUInt256.of(details.numberNft),
                    recipient = details.nftRecipient,
                    date = date,
                    source = HistorySource.SUDOSWAP
                )
            }
            is SudoSwapTargetOutNftDetail -> {
                PoolTargetNftOut(
                    hash = hash,
                    nftIds = details.nft.map { EthUInt256.of(it) },
                    recipient = details.nftRecipient,
                    date = date,
                    source = HistorySource.SUDOSWAP
                )
            }
        }
    }
}
