package com.rarible.protocol.nftorder.core.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemId
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.Part
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address

fun randomEthUInt256() = EthUInt256.of(randomWord())

fun randomPart() = Part(randomAddress(), randomInt())
fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())

fun randomOwnership() = randomOwnership(randomItemId(), randomPart())
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

fun randomAssetErc721(itemId: ItemId) = AssetDto(
    Erc721AssetTypeDto(itemId.token, itemId.tokenId.value),
    randomBigInt()
)

fun randomAssetErc20() = randomAssetErc20(randomAddress())
fun randomAssetErc20(address: Address) = AssetDto(Erc20AssetTypeDto(address), randomBigInt())

fun randomOrderDto(itemId: ItemId) = randomOrderDto(itemId, randomAddress())
fun randomOrderDto(itemId: ItemId, maker: Address) = randomOrderDto(
    randomAssetErc721(itemId), maker, randomAssetErc20()
)

fun randomOrderDto(make: AssetDto, maker: Address, take: AssetDto): LegacyOrderDto {
    return LegacyOrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        makeStock = randomBigInt(),
        cancelled = false,
        salt = Word.apply(RandomUtils.nextBytes(32)),
        data = OrderDataLegacyDto(randomInt()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(RandomUtils.nextBytes(32)),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = null,
        end = null,
        priceHistory = listOf()
    )
}