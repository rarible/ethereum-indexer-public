package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrder(): Order {
    return Order(
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN),
        take = Asset(
            Erc20AssetType(AddressFactory.create()),
            EthUInt256.of(BigInteger.valueOf(5) * BigInteger.valueOf(10).pow(18))
        ),
        makeStock = EthUInt256.TEN,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        start = null,
        end = null,
        signature = null
    )
}
