package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrder(
    token: Address = AddressFactory.create(),
    tokenId: EthUInt256 = EthUInt256.TEN,
): Order {
    return Order(
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(Erc1155AssetType(token, tokenId), EthUInt256.TEN),
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

fun createOrderBid(): Order {
    return Order(
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(
            Erc20AssetType(AddressFactory.create()),
            EthUInt256.of(BigInteger.valueOf(5) * BigInteger.valueOf(10).pow(18))
        ),
        take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN),
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
        signature = null,
        platform = Platform.RARIBLE,
        id = Order.Id(Word.apply(randomWord())),
    )
}
