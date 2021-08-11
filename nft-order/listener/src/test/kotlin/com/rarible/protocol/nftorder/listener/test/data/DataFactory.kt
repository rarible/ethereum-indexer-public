package com.rarible.protocol.nftorder.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import kotlin.math.abs

fun randomString(): String {
    return RandomStringUtils.randomAlphabetic(8)
}

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

fun randomBigInt(): BigInteger {
    return BigInteger.valueOf(randomLong())
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

fun randomPartDto() = randomPartDto(randomAddress())

fun randomPartDto(account: Address) = PartDto(account, randomPositiveInt())

fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())

fun randomOwnershipId(itemId: ItemId) = OwnershipId(itemId.token, itemId.tokenId, randomAddress())

fun randomOwnershipId() = OwnershipId(randomAddress(), randomEthUInt256(), randomAddress())

fun randomItem(): Item {
    return randomItem(randomPart())
}

fun randomItem(vararg creators: Part): Item {
    return Item(
        token = randomAddress(),
        tokenId = randomEthUInt256(),
        creators = creators.asList(),
        supply = randomEthUInt256(),
        lazySupply = randomEthUInt256(),
        royalties = emptyList(),
        owners = emptyList(),
        date = nowMillis(),
        pending = emptyList(),
        totalStock = BigInteger.ZERO,
        bestBidOrder = null,
        bestSellOrder = null,
        unlockable = false
    )
}

fun randomNftItemDto(): NftItemDto {
    return randomNftItemDto(randomItemId(), randomPartDto())
}

fun randomNftItemDto(itemId: ItemId, vararg creators: PartDto): NftItemDto {
    return NftItemDto(
        id = itemId.decimalStringValue,
        contract = itemId.token,
        tokenId = itemId.tokenId.value,
        creators = creators.asList(),
        supply = randomPositiveBigInt(),
        lazySupply = randomPositiveBigInt(),
        royalties = emptyList(),
        date = nowMillis(),
        owners = emptyList(),
        pending = emptyList(),
        deleted = false,
        meta = null
    )
}

fun randomOwnership(): Ownership {
    return randomOwnership(randomItemId(), randomPart())
}

fun randomOwnership(item: Item): Ownership {
    return randomOwnership(item.id, item.creators.first())
}

fun randomOwnership(itemId: ItemId): Ownership {
    return randomOwnership(itemId, randomPart())
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

fun randomNftOwnershipDto(): NftOwnershipDto {
    return randomNftOwnershipDto(randomOwnershipId())
}

fun randomNftOwnershipDto(itemId: ItemId): NftOwnershipDto {
    return randomNftOwnershipDto(itemId, randomPartDto())
}

fun randomNftOwnershipDto(nftItem: NftItemDto): NftOwnershipDto {
    return randomNftOwnershipDto(ItemId.parseId(nftItem.id), nftItem.creators.first())
}

fun randomNftOwnershipDto(ownershipId: OwnershipId): NftOwnershipDto {
    return randomNftOwnershipDto(
        ItemId(ownershipId.token, ownershipId.tokenId),
        PartDto(ownershipId.owner, randomPositiveInt())
    )
}

fun randomNftOwnershipDto(itemId: ItemId, creator: PartDto): NftOwnershipDto {
    val ownershipId = OwnershipId(itemId.token, itemId.tokenId, creator.account)
    return NftOwnershipDto(
        id = ownershipId.stringValue,
        contract = ownershipId.token,
        tokenId = ownershipId.tokenId.value,
        owner = ownershipId.owner,
        creators = listOf(creator),
        value = randomPositiveBigInt(),
        lazyValue = randomPositiveBigInt(),
        date = nowMillis(),
        pending = emptyList()
    )
}

fun randomAssetErc721() = randomAssetErc721(randomItemId())

fun randomAssetErc721(itemId: ItemId) =
    AssetDto(Erc721AssetTypeDto(itemId.token, itemId.tokenId.value), randomPositiveBigInt())

fun randomAssetErc20() = randomAssetErc20(randomAddress())

fun randomAssetErc20(address: Address) = AssetDto(Erc20AssetTypeDto(address), randomPositiveBigInt())

fun randomAssetErc1155() = randomAssetErc721(randomItemId())

fun randomAssetErc1155(itemId: ItemId) =
    AssetDto(Erc1155AssetTypeDto(itemId.token, itemId.tokenId.value), randomPositiveBigInt())

fun randomOrderDto() = randomOrderDto(randomAssetErc721(), randomAddress(), randomAssetErc20())

fun randomOrderDto(itemId: ItemId) = randomOrderDto(itemId, randomAddress())

fun randomOrderDto(itemId: ItemId, maker: Address) =
    randomOrderDto(randomAssetErc721(itemId), maker, randomAssetErc20())

fun randomOrderDto(itemId: ItemId, maker: Address, take: Address) =
    randomOrderDto(randomAssetErc721(itemId), maker, randomAssetErc20(take))

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