package com.rarible.protocol.gateway.test

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address

fun randomEthUInt256() = EthUInt256.of(randomWord())

data class ItemId(
    val token: Address,
    val tokenId: EthUInt256
)

fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())

fun randomAssetErc721(itemId: ItemId) = AssetDto(
    Erc721AssetTypeDto(itemId.token, itemId.tokenId.value),
    randomBigInt()
)

fun randomAssetErc1155() = randomAssetErc721(randomItemId())

fun randomTransferDto(): TransferDto {
    return TransferDto(
        date = nowMillis(),
        id = randomString(),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(RandomUtils.nextBytes(32)),
        from = randomAddress(),
        blockHash = Word.apply(RandomUtils.nextBytes(32)),
        blockNumber = randomLong(),
        logIndex = randomInt()
    )
}

fun randomOrderBidActivityDto(): OrderActivityBidDto {
    return OrderActivityBidDto(
        date = nowMillis(),
        id = randomString(),
        maker = randomAddress(),
        make = randomAssetErc1155(),
        take = randomAssetErc1155(),
        price = randomBigInt().toBigDecimal(),
        priceUsd = randomBigInt().toBigDecimal(),
        hash = Word.apply(RandomUtils.nextBytes(32)),
        source = OrderActivityDto.Source.RARIBLE
    )
}