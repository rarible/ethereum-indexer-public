package com.rarible.protocol.order.core.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createOrder() =
    Order(
        maker = AddressFactory.create(),
        taker = null,
        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = EthUInt256.TEN,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = EthUInt256.TEN,
        start = null,
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )

fun createOrderDto() =
    RaribleV2OrderDto(
        maker = AddressFactory.create(),
        taker = null,
        make = AssetDto(Erc20AssetTypeDto(AddressFactory.create()), BigInteger.TEN),
        take = AssetDto(Erc20AssetTypeDto(AddressFactory.create()), BigInteger.TEN),
        fill = BigInteger.ZERO,
        makeStock = BigInteger.TEN,
        cancelled = false,
        salt = Word.apply(ByteArray(32)),
        data = OrderRaribleV2DataV1Dto(emptyList(), emptyList()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = WordFactory.create(),
        makeBalance = BigInteger.TEN,
        makePriceUsd = null,
        takePriceUsd = null,
        start = null,
        end = null
    )
