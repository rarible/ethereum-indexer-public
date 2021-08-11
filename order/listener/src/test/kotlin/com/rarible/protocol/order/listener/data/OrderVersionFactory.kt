package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.OrderVersion
import io.daonomic.rpc.domain.Word
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrderVersion(): OrderVersion {
    return OrderVersion(
        hash = Word.apply(ByteArray(32)),
        maker = AddressFactory.create(),
        taker = AddressFactory.create(),
        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        take = Asset(
            Erc20AssetType(AddressFactory.create()),
            EthUInt256.of(BigInteger.valueOf(5) * BigInteger.valueOf(10).pow(18))
        ),
        makePriceUsd = (1..100L).random().toBigDecimal(),
        takePriceUsd = (1..100L).random().toBigDecimal(),
        makeUsd = (1..100L).random().toBigDecimal(),
        takeUsd = (1..100L).random().toBigDecimal(),
        createdAt = nowMillis()
    )
}
