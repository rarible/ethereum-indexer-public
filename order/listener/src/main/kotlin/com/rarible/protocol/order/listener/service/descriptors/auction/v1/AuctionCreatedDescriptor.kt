package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.auction.v1.AuctionCreatedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
class AuctionCreatedDescriptor(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses
) : LogEventDescriptor<OnChainAuction> {

    private val auctionContract = auctionContractAddresses.v1

    override val collection: String
        get() = AuctionHistoryRepository.COLLECTION

    override val topic: Word = AuctionCreatedEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<OnChainAuction> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private fun convert(log: Log, date: Instant): List<OnChainAuction> {
        val event = AuctionCreatedEvent.apply(log)
        val contract = log.address()
        val auctionId = EthUInt256.of(event.auctionId())
        val auction = event.auction()

        val sell = Asset(auction._1()._1().toAssetType(), EthUInt256(auction._1()._2()))
        val buy = auction._2().toAssetType()
        val lastBid = auction._3().takeUnless { bid -> bid._2().isEmpty() }?.toAuctionBid()
        val seller = auction._4()
        val buyer = auction._5().takeUnless { buyer -> buyer == Address.ZERO() }
        val endTime = auction._6().takeUnless { endTime -> endTime == BigInteger.ZERO }
        val minimalStep = auction._7()
        val minimalPrice = auction._8()
        val protocolFee = auction._9()
        val data = AuctionData.decode(Binary.apply(auction._10()), Binary.apply(auction._11()))

        return listOf(
            OnChainAuction(
                auctionType = AuctionType.RARIBLE_V1,
                seller = seller,
                buyer = buyer,
                sell = sell,
                buy = buy,
                lastBid = lastBid,
                endTime = endTime?.let { Instant.ofEpochSecond(endTime.toLong()) },
                minimalStep = EthUInt256.of(minimalStep),
                minimalPrice = EthUInt256.of(minimalPrice),
                data = data,
                protocolFee = EthUInt256.of(protocolFee),
                createdAt = date,
                date = date,
                contract = contract,
                auctionId = auctionId,
                hash = Auction.raribleV1HashKey(contract, auctionId)
            )
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(auctionContract))
    }
}
