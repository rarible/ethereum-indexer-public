package com.rarible.protocol.order.core.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionCancelled
import com.rarible.protocol.order.core.model.AuctionFinished
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.AuctionStatus
import com.rarible.protocol.order.core.model.AuctionType
import com.rarible.protocol.order.core.model.BidDataV1
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.BidV1
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OnChainAuction
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.RaribleAuctionV1DataV1
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

fun randomAuction(): Auction {
    return Auction(
        type = AuctionType.RARIBLE_V1,
        status = AuctionStatus.ACTIVE,
        seller = randomAddress(),
        buyer = randomAddress(),
        sell = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE),
        buy = EthAssetType,
        lastBid = null,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH,
        minimalStep = EthUInt256.ZERO,
        minimalPrice = EthUInt256.ZERO,
        ongoing = false,
        finished = false,
        cancelled = false,
        deleted = false,
        data = randomAuctionV1DataV1(),
        createdAt = Instant.EPOCH,
        lastUpdateAt = Instant.EPOCH,
        lastEventId = null,
        auctionId = EthUInt256.of(randomBigInt()),
        protocolFee = EthUInt256.ZERO,
        contract = randomAddress(),
        pending = emptyList(),
        buyPrice = randomBigDecimal(),
        buyPriceUsd = randomBigDecimal(),
        platform = Platform.RARIBLE
    )
}

fun randomBid(): BidV1 {
    return BidV1(
        amount = EthUInt256.of(randomBigInt()),
        data = BidDataV1(emptyList(), emptyList())
    )
}

fun randomAuctionV1DataV1(): RaribleAuctionV1DataV1 {
    return RaribleAuctionV1DataV1(
        originFees = emptyList(),
        payouts = emptyList(),
        duration = EthUInt256.ZERO,
        startTime = Instant.EPOCH,
        buyOutPrice = EthUInt256.ZERO
    )
}

fun randomAuctionCreated(): OnChainAuction {
    val contract = randomAddress()
    val auctionId = EthUInt256.ONE
    return randomAuctionCreated(contract, auctionId)
}

fun randomAuctionCreated(contract: Address, auctionId: EthUInt256): OnChainAuction {
    return OnChainAuction(
        auctionType = AuctionType.RARIBLE_V1,
        seller = randomAddress(),
        buyer = null,
        sell = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE),
        buy = Erc721AssetType(randomAddress(), EthUInt256.ONE),
        lastBid = null,
        endTime = Instant.now(),
        minimalStep = EthUInt256.ONE,
        minimalPrice = EthUInt256.ONE,
        data = randomAuctionV1DataV1(),
        protocolFee = EthUInt256.ZERO,
        createdAt = Instant.now(),
        auctionId = auctionId,
        contract = contract,
        hash = Auction.raribleV1HashKey(contract, auctionId),
        date = Instant.now(),
        source = HistorySource.RARIBLE
    )
}

fun randomBidPlaced(): BidPlaced {
    val contract = randomAddress()
    val auctionId = EthUInt256.ONE
    return randomBidPlaced(contract, auctionId)
}

fun randomBidPlaced(contract: Address, auctionId: EthUInt256): BidPlaced {
    return BidPlaced(
        bid = randomBid(),
        buyer = randomAddress(),
        endTime = EthUInt256.ZERO,
        auctionId = auctionId,
        sell = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE),
        bidValue = BigDecimal.ONE,
        hash = Auction.raribleV1HashKey(contract, auctionId),
        contract = contract,
        date = Instant.now(),
        source = HistorySource.RARIBLE
    )
}

fun randomCanceled(): AuctionCancelled {
    val contract = randomAddress()
    val auctionId = EthUInt256.ONE
    return randomCanceled(contract, auctionId)
}

fun randomCanceled(contract: Address, auctionId: EthUInt256): AuctionCancelled {
    return AuctionCancelled(
        auctionId = auctionId,
        seller = null,
        sell = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE),
        hash = Auction.raribleV1HashKey(contract, auctionId),
        contract = contract,
        date = Instant.now(),
        source = HistorySource.RARIBLE
    )
}

fun randomFinished(): AuctionFinished {
    val contract = randomAddress()
    val auctionId = EthUInt256.ONE
    return randomFinished(contract, auctionId)
}

fun randomFinished(contract: Address, auctionId: EthUInt256): AuctionFinished {
    return AuctionFinished(
        seller = randomAddress(),
        buyer = null,
        sell = Asset(Erc721AssetType(randomAddress(), EthUInt256.ONE), EthUInt256.ONE),
        buy = Erc721AssetType(randomAddress(), EthUInt256.ONE),
        lastBid = null,
        endTime = Instant.now(),
        minimalStep = EthUInt256.ONE,
        minimalPrice = EthUInt256.ONE,
        data = randomAuctionV1DataV1(),
        protocolFee = EthUInt256.ZERO,
        createdAt = Instant.now(),
        source = HistorySource.RARIBLE,
        auctionId = auctionId,
        contract = contract,
        hash = Auction.raribleV1HashKey(contract, auctionId),
        date = Instant.now(),
    )
}

fun createAuctionLogEvent(data: AuctionHistory) = LogEvent(
    data = data,
    address = randomAddress(),
    topic = Word.apply(RandomUtils.nextBytes(32)),
    transactionHash = Word.apply(RandomUtils.nextBytes(32)),
    index = RandomUtils.nextInt(),
    minorLogIndex = 0,
    status = LogEventStatus.CONFIRMED
)

fun randomLogList(auctions: List<Auction>): List<LogEvent> {
    return auctions.flatMap { auction ->
        listOf(
            createAuctionLogEvent(randomAuctionCreated().copy(sell = auction.sell)),
            createAuctionLogEvent(randomBidPlaced(auction.contract, auction.auctionId).copy(sell = auction.sell)),
            createAuctionLogEvent(randomCanceled().copy(sell = auction.sell)),
            createAuctionLogEvent(randomFinished().copy(sell = auction.sell))
        )
    }
}

fun createOffchainHistoryEvent(auction: Auction, type: AuctionOffchainHistory.Type) = AuctionOffchainHistory(
    hash = auction.hash,
    date = auction.startTime ?: auction.createdAt,
    contract = auction.contract,
    seller = auction.seller,
    sell = auction.sell,
    source = HistorySource.RARIBLE,
    type = type
)
