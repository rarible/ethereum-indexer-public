package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scala.Tuple11
import scala.Tuple2
import scala.Tuple3
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

abstract class AbstractAuctionDescriptor<T : EventData>(
    auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses
) : LogEventDescriptor<T> {

    private val auctionContract = auctionContractAddresses.v1

    override val collection: String
        get() = AuctionHistoryRepository.COLLECTION

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<T> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    protected abstract fun convert(log: Log, date: Instant): List<T>

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(listOf(auctionContract))
    }

    protected companion object {
        fun parseContractAuction(auction: Tuple11<Tuple2<Tuple2<ByteArray, ByteArray>, BigInteger>, Tuple2<ByteArray, ByteArray>, Tuple3<BigInteger, ByteArray, ByteArray>, Address, Address, BigInteger, BigInteger, BigInteger, BigInteger, ByteArray, ByteArray>): ContractAuction {
            val sell = Asset(auction._1()._1().toAssetType(), EthUInt256(auction._1()._2()))
            val buy = auction._2().toAssetType()
            val lastBid = auction._3().takeUnless { bid -> bid._1() == BigInteger.ZERO }?.toAuctionBid()
            val seller = auction._4()
            val buyer = auction._5().takeUnless { buyer -> buyer == Address.ZERO() }
            val endTime = auction._6().takeUnless { endTime -> endTime == BigInteger.ZERO }
            val minimalStep = auction._7()
            val minimalPrice = auction._8()
            val protocolFee = auction._9()
            val data = AuctionData.decode(Binary.apply(auction._10()), Binary.apply(auction._11()))

            return ContractAuction(
                seller = seller,
                buyer = buyer,
                sell = sell,
                buy = buy,
                lastBid = lastBid,
                endTime = endTime?.let { Instant.ofEpochSecond(endTime.toLong()) },
                minimalStep = EthUInt256.of(minimalStep),
                minimalPrice = EthUInt256.of(minimalPrice),
                data = data,
                protocolFee = EthUInt256.of(protocolFee)
            )
        }

        data class ContractAuction(
            val seller: Address,
            val buyer: Address?,
            val sell: Asset,
            val buy: AssetType,
            val lastBid: Bid?,
            val endTime: Instant?,
            val minimalStep: EthUInt256,
            val minimalPrice: EthUInt256,
            val protocolFee: EthUInt256,
            val data: AuctionData
        )
    }
}
