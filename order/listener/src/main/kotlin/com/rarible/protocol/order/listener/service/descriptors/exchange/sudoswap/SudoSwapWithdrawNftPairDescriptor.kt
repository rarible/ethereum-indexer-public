package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.NFTWithdrawalEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolNftWithdraw
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
class SudoSwapWithdrawNftPairDescriptor(
    private val sudoSwapEventConverter: SudoSwapEventConverter
): LogEventDescriptor<PoolNftWithdraw> {

    override val collection: String = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = NFTWithdrawalEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolNftWithdraw> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): PoolNftWithdraw {
        val details = sudoSwapEventConverter.getNftWithdrawDetails(transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        return PoolNftWithdraw(
            hash = sudoSwapEventConverter.getPoolHash(log.address()),
            collection = details.collection,
            nftIds = details.nft.map { EthUInt256.of(it) },
            date = date,
            source = HistorySource.SUDOSWAP
        )
    }
}