package com.rarible.protocol.order.api.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import java.math.BigDecimal
import java.time.Instant

fun randomAuction(): Auction {
    return Auction(
        type = AuctionType.RARIBLE_V1,
        status = AuctionStatus.ACTIVE,
        seller = randomAddress(),
        buyer = randomAddress(),
        sell = Asset(EthAssetType, EthUInt256.of(randomBigInt())),
        buy = EthAssetType,
        lastBid = null,
        endTime = Instant.EPOCH,
        minimalStep = EthUInt256.ZERO,
        minimalPrice = EthUInt256.ZERO,
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
        startTime = EthUInt256.ZERO,
        buyOutPrice = EthUInt256.ZERO
    )
}

fun randomBidPlaced(): BidPlaced {
    return BidPlaced(
        bid = randomBid(),
        buyer = randomAddress(),
        endTime = EthUInt256.ZERO,
        auctionId = EthUInt256.ONE,
        bidValue = BigDecimal.ONE,
        hash = Word.apply(randomWord()),
        contract = randomAddress(),
        date = Instant.now(),
        source = HistorySource.RARIBLE
    )
}


fun createAuctionLogEvent(data: AuctionHistory) = LogEvent(
    data = data,
    address = createAddress(),
    topic = Word.apply(RandomUtils.nextBytes(32)),
    transactionHash = Word.apply(RandomUtils.nextBytes(32)),
    index = RandomUtils.nextInt(),
    minorLogIndex = 0,
    status = LogEventStatus.CONFIRMED
)
