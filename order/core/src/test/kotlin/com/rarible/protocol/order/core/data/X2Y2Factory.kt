package com.rarible.protocol.order.listener.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.x2y2.client.model.ErcType
import com.rarible.x2y2.client.model.Event
import com.rarible.x2y2.client.model.EventType
import com.rarible.x2y2.client.model.Order
import com.rarible.x2y2.client.model.OrderStatus
import com.rarible.x2y2.client.model.OrderType
import com.rarible.x2y2.client.model.Token
import io.daonomic.rpc.domain.Word
import java.time.Instant

fun randomX2Y2Order(): Order {
    return Order(
        amount = randomBigInt(),
        id = randomBigInt(),
        maker = randomAddress(),
        currency = randomAddress(),
        taker = randomAddress(),
        price = randomBigInt(),
        createdAt = Instant.now(),
        itemHash = Word.apply(randomWord()),
        type = OrderType.values().random(),
        isCollectionOffer = randomBoolean(),
        endAt = Instant.now(),
        isBundle = randomBoolean(),
        side = randomInt(),
        status = OrderStatus.values().random(),
        token = Token(
            contract = randomAddress(),
            tokenId = randomBigInt(),
            ercType = ErcType.values().random()
        ),
        updatedAt = Instant.now()
    )
}

fun randomX2Y2Event(): Event {
    return Event(
        id = randomBigInt(),
        createdAt = Instant.now(),
        type = EventType.values().random(),
        token = Token(
            contract = randomAddress(),
            tokenId = randomBigInt(),
            ercType = ErcType.values().random()
        ),
        order = randomX2Y2Order(),
        fromAddress = randomAddress(),
        toAddress = randomAddress(),
        tx = randomString()
    )
}
