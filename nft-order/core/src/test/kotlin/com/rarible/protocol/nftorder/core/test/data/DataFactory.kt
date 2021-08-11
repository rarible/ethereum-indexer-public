package com.rarible.protocol.nftorder.core.test.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.Part
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import kotlin.math.abs

fun randomInt(): Int {
    return RandomUtils.nextInt()
}

fun randomPositiveInt(): Int {
    return abs(randomInt())
}

fun randomLong(): Long {
    return RandomUtils.nextLong()
}

fun randomPositiveLong(): Long {
    return abs(randomLong())
}

fun randomPositiveBigInt(): BigInteger {
    return BigInteger.valueOf(randomPositiveLong())
}

fun randomAddress(): Address {
    return AddressFactory.create()
}

fun randomEthUInt256(): EthUInt256 {
    return EthUInt256(randomPositiveBigInt())
}

fun randomPart() = Part(randomAddress(), randomPositiveInt())

fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())

fun randomOwnership(): Ownership {
    return randomOwnership(randomItemId(), randomPart())
}

fun randomOwnership(itemId: ItemId, creator: Part): Ownership {
    return Ownership(
        contract = itemId.token,
        tokenId = itemId.tokenId,
        creators = listOf(creator),
        owner = creator.account,
        value = randomEthUInt256(),
        date = nowMillis(),
        pending = emptyList(),
        bestSellOrder = null
    )
}

fun randomAssetErc721(itemId: ItemId) =
    AssetDto(Erc721AssetTypeDto(itemId.token, itemId.tokenId.value), randomPositiveBigInt())

fun randomAssetErc20() = randomAssetErc20(randomAddress())

fun randomAssetErc20(address: Address) = AssetDto(Erc20AssetTypeDto(address), randomPositiveBigInt())

fun randomOrderDto(itemId: ItemId) = randomOrderDto(itemId, randomAddress())

fun randomOrderDto(itemId: ItemId, maker: Address) =
    randomOrderDto(randomAssetErc721(itemId), maker, randomAssetErc20())

fun randomOrderDto(make: AssetDto, maker: Address, take: AssetDto): OrderDto {
    return OrderDto(
        type = OrderTypeDto.RARIBLE_V2,
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomPositiveBigInt(),
        makeStock = randomPositiveBigInt(),
        cancelled = false,
        salt = Binary.apply(),
        data = OrderDataLegacyDto(randomInt()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(RandomUtils.nextBytes(32)),
        makeBalance = randomPositiveBigInt(),
        makePriceUsd = randomPositiveBigInt().toBigDecimal(),
        takePriceUsd = randomPositiveBigInt().toBigDecimal(),
        start = null,
        end = null
    )
}