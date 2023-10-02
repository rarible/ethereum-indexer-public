package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.AuctionData
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.Bid
import com.rarible.protocol.order.core.model.BidData
import com.rarible.protocol.order.core.model.BidDataV1
import com.rarible.protocol.order.core.model.BidV1
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AuctionSubscriber
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scala.Tuple11
import scala.Tuple2
import scala.Tuple3
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@CaptureSpan(type = SpanType.EVENT)
abstract class AbstractAuctionDescriptor<T : AuctionHistory>(
    name: String,
    topic: Word,
    contractsProvider: ContractsProvider,
    private val autoReduceService: AutoReduceService,
) : AuctionSubscriber<T>(
    name = name,
    topic = topic,
    contracts = contractsProvider.raribleAuctionV1(),
    autoReduceService = autoReduceService,
) {
    protected companion object {

        fun toBid(tuple: Tuple3<BigInteger, ByteArray, ByteArray>, date: Instant): Bid {
            return when (val bidData = BidData.decode(Binary.apply(tuple._2()), Binary.apply(tuple._3()))) {
                is BidDataV1 -> BidV1(EthUInt256.of(tuple._1()), bidData, date)
            }
        }

        fun parseContractAuction(
            auction: Tuple11<Tuple2<Tuple2<ByteArray, ByteArray>, BigInteger>, Tuple2<ByteArray, ByteArray>, Tuple3<BigInteger, ByteArray, ByteArray>, Address, Address, BigInteger, BigInteger, BigInteger, BigInteger, ByteArray, ByteArray>,
            date: Instant
        ): ContractAuction {
            val sell = Asset(auction._1()._1().toAssetType(), EthUInt256(auction._1()._2()))
            val buy = auction._2().toAssetType()
            val lastBid = auction._3().takeUnless { bid -> bid._1() == BigInteger.ZERO }?.let { toBid(it, date) }
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
