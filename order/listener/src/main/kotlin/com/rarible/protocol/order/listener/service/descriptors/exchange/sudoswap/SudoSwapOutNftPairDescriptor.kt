package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.SwapNFTOutPairEvent
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import com.rarible.protocol.order.core.model.SudoSwapAnyOutNftDetail
import com.rarible.protocol.order.core.model.SudoSwapTargetOutNftDetail
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapNftTransferDetector
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
    private val sudoSwapEventConverter: SudoSwapEventConverter,
    private val nftTransferDetector: SudoSwapNftTransferDetector,
    private val orderRepository: OrderRepository
): LogEventDescriptor<PoolTargetNftOut> {

    override val collection: String = PoolHistoryRepository.COLLECTION

    override val topic: Word = SwapNFTOutPairEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> = emptyList<Address>().toMono()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<PoolTargetNftOut> {
        return mono { listOfNotNull(convert(log, transaction, index, totalLogs, Instant.ofEpochSecond(timestamp))) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, index: Int, totalLogs: Int, date: Instant): PoolTargetNftOut {
        val details = sudoSwapEventConverter.getSwapOutNftDetails(transaction).let {
            assert(it.size == totalLogs)
            it[index]
        }
        val hash = sudoSwapEventConverter.getPoolHash(log.address())
        return when (details) {
            is SudoSwapAnyOutNftDetail -> {
                val order = orderRepository.findById(hash)
                    ?: throw IllegalStateException("Can't find pool order $hash, nftOutLog=$log")
                val nftCollection =
                    order.make.type.token.takeUnless { it == Address.ZERO() }
                    ?: order.take.type.token.takeUnless { it == Address.ZERO() }
                    ?: throw IllegalStateException("Can't find pool $hash nftCollection")

                val tokenIds = nftTransferDetector.detectNftTransfers(
                    sudoSwapNftOutPairLog = log,
                    nftCollection = nftCollection
                )
                require(tokenIds.size == details.numberNft.toInt()) {
                    "Found tokenIds amount didn't much event nft out number"
                }
                PoolTargetNftOut(
                    hash = hash,
                    tokenIds = tokenIds.map { EthUInt256.of(it) },
                    recipient = details.nftRecipient,
                    date = date,
                    source = HistorySource.SUDOSWAP
                )
            }
            is SudoSwapTargetOutNftDetail -> {
                PoolTargetNftOut(
                    hash = hash,
                    tokenIds = details.nft.map { EthUInt256.of(it) },
                    recipient = details.nftRecipient,
                    date = date,
                    source = HistorySource.SUDOSWAP
                )
            }
        }
    }
}
