package com.rarible.protocol.order.listener.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.opensea.client.model.v2.Account
import com.rarible.opensea.client.model.v2.Consideration
import com.rarible.opensea.client.model.v2.Fee
import com.rarible.opensea.client.model.v2.ItemType
import com.rarible.opensea.client.model.v2.Offer
import com.rarible.opensea.client.model.v2.OrderParameters
import com.rarible.opensea.client.model.v2.OrderSide
import com.rarible.opensea.client.model.v2.OrderType
import com.rarible.opensea.client.model.v2.ProtocolData
import com.rarible.opensea.client.model.v2.SeaportOrder
import com.rarible.opensea.client.model.v2.SeaportOrderType
import io.daonomic.rpc.domain.Word
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

fun randomOffer(): Offer {
    return Offer(
        itemType = ItemType.values().random(),
        token = randomAddress(),
        identifierOrCriteria = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt()
    )
}

fun randomConsideration(): Consideration {
    return Consideration(
        itemType = ItemType.values().random(),
        token = randomAddress(),
        identifierOrCriteria = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt(),
        recipient = randomAddress()
    )
}

fun randomOrderParameters(): OrderParameters {
    return OrderParameters(
        offerer = randomAddress(),
        zone = randomAddress(),
        offer = listOf(randomOffer()),
        consideration = listOf(randomConsideration()),
        orderType = OrderType.values().random(),
        startTime = randomBigInt(),
        endTime = randomBigInt(),
        zoneHash = Word.apply(randomWord()),
        salt = randomBigInt(),
        conduitKey = Word.apply(randomWord()),
        totalOriginalConsiderationItems = randomBigInt(),
        counter = BigInteger.valueOf(randomLong())
    )
}

fun randomProtocolData(): ProtocolData {
    return ProtocolData(
        parameters = randomOrderParameters(),
        signature = randomBinary(32)
    )
}

fun randomAccount(): Account {
    return Account(
        address = randomString()
    )
}

fun randomFee(): Fee {
    return Fee(randomAccount(), randomLong())
}

fun randomSeaportOrder(): SeaportOrder {
    return SeaportOrder(
        createdAt = Instant.now(),
        closingDate = Instant.now() + Duration.ofDays(1),
        listingTime = randomLong(),
        expirationTime = randomLong(),
        orderHash = Word.apply(randomWord()),
        protocolData = randomProtocolData(),
        protocolAddress = randomAddress(),
        maker = randomAccount(),
        taker = randomAccount(),
        currentPrice = randomBigInt(),
        makerFees = listOf(randomFee()),
        takerFees = listOf(randomFee()),
        side = OrderSide.SELL,
        orderType = SeaportOrderType.BASIC,
        canceled = false,
        finalized = false,
        markedInvalid = false,
        clientSignature = randomBinary(32)
    )
}
