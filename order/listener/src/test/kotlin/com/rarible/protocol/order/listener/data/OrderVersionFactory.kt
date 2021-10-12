package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrderVersion(): OrderVersion {
    return OrderVersion(
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        take = Asset(
            Erc20AssetType(AddressFactory.create()),
            EthUInt256.of(BigInteger.valueOf(5) * BigInteger.valueOf(10).pow(18))
        ),
        makePriceUsd = (1..100L).random().toBigDecimal(),
        takePriceUsd = (1..100L).random().toBigDecimal(),
        makePrice = (1..100L).random().toBigDecimal(),
        takePrice = (1..100L).random().toBigDecimal(),
        makeUsd = (1..100L).random().toBigDecimal(),
        takeUsd = (1..100L).random().toBigDecimal(),
        createdAt = nowMillis(),
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null
    )
}
