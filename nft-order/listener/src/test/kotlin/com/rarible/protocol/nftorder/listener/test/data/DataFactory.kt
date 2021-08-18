package com.rarible.protocol.nftorder.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.*
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import java.math.BigInteger

fun randomEthUInt256() = EthUInt256.of(randomWord())

fun randomPart() = Part(randomAddress(), randomInt())
fun randomPartDto(account: Address) = PartDto(account, randomInt())

fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())
fun randomOwnershipId(itemId: ItemId) = OwnershipId(itemId.token, itemId.tokenId, randomAddress())
fun randomOwnershipId() = OwnershipId(randomAddress(), randomEthUInt256(), randomAddress())

fun randomItem() = randomItem(randomPart())
fun randomItem(itemId: ItemId) = randomItem(itemId, randomPart())
fun randomItem(vararg creators: Part) = randomItem(randomItemId(), *creators)
fun randomItem(itemId: ItemId, vararg creators: Part): Item {
    return Item(
        token = itemId.token,
        tokenId = itemId.tokenId,
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

fun randomNftItemDto(itemId: ItemId, vararg creators: PartDto): NftItemDto {
    return NftItemDto(
        id = itemId.decimalStringValue,
        contract = itemId.token,
        tokenId = itemId.tokenId.value,
        creators = creators.asList(),
        supply = randomBigInt(),
        lazySupply = randomBigInt(),
        royalties = emptyList(),
        date = nowMillis(),
        owners = emptyList(),
        pending = emptyList(),
        deleted = false,
        meta = null
    )
}

fun randomOwnership() = randomOwnership(randomItemId(), randomPart())
fun randomOwnership(item: Item) = randomOwnership(item.id, item.creators.first())
fun randomOwnership(itemId: ItemId) = randomOwnership(itemId, randomPart())
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

fun randomNftOwnershipDto(nftItem: NftItemDto) = randomNftOwnershipDto(
    ItemId.parseId(nftItem.id), nftItem.creators.first()
)

fun randomNftOwnershipDto(ownershipId: OwnershipId) = randomNftOwnershipDto(
    ItemId(ownershipId.token, ownershipId.tokenId),
    PartDto(ownershipId.owner, randomInt())
)

fun randomNftOwnershipDto(itemId: ItemId, creator: PartDto): NftOwnershipDto {
    val ownershipId = OwnershipId(itemId.token, itemId.tokenId, creator.account)
    return NftOwnershipDto(
        id = ownershipId.stringValue,
        contract = ownershipId.token,
        tokenId = ownershipId.tokenId.value,
        owner = ownershipId.owner,
        creators = listOf(creator),
        value = randomBigInt(),
        lazyValue = randomBigInt(),
        date = nowMillis(),
        pending = emptyList()
    )
}

fun randomAssetErc721() = randomAssetErc721(randomItemId())

fun randomAssetErc721(itemId: ItemId) = AssetDto(
    Erc721AssetTypeDto(itemId.token, itemId.tokenId.value),
    randomBigInt()
)

fun randomAssetErc20() = randomAssetErc20(randomAddress())
fun randomAssetErc20(address: Address) = AssetDto(Erc20AssetTypeDto(address), randomBigInt())

fun randomAssetErc1155(itemId: ItemId) = AssetDto(
    Erc1155AssetTypeDto(itemId.token, itemId.tokenId.value),
    randomBigInt()
)

fun randomLegacyOrderDto() = randomLegacyOrderDto(randomAssetErc721(), randomAddress(), randomAssetErc20())
fun randomLegacyOrderDto(itemId: ItemId) = randomLegacyOrderDto(itemId, randomAddress())
fun randomLegacyOrderDto(itemId: ItemId, maker: Address) = randomLegacyOrderDto(
    randomAssetErc721(itemId),
    maker,
    randomAssetErc20()
)

fun randomLegacyOrderDto(make: AssetDto, maker: Address, take: AssetDto): LegacyOrderDto {
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
        end = null
    )
}

fun randomOpenSeaV1OrderDto(itemId: ItemId) = randomOpenSeaV1OrderDto(itemId, randomAddress())
fun randomOpenSeaV1OrderDto(itemId: ItemId, maker: Address) = randomOpenSeaV1OrderDto(
    randomAssetErc721(itemId),
    maker,
    randomAssetErc20()
)

fun randomOpenSeaV1OrderDto(make: AssetDto, maker: Address, take: AssetDto): OpenSeaV1OrderDto {
    return OpenSeaV1OrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        makeStock = randomBigInt(),
        cancelled = false,
        salt = Word.apply(RandomUtils.nextBytes(32)),
        data = randomOrderOpenSeaV1DataV1Dto(),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(RandomUtils.nextBytes(32)),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = null,
        end = null
    )
}

fun randomOrderOpenSeaV1DataV1Dto(): OrderOpenSeaV1DataV1Dto {
    return OrderOpenSeaV1DataV1Dto(
        exchange = randomAddress(),
        makerRelayerFee = randomBigInt(),
        takerRelayerFee = randomBigInt(),
        makerProtocolFee = randomBigInt(),
        takerProtocolFee = randomBigInt(),
        feeRecipient = randomAddress(),
        feeMethod = OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE,
        side = OrderOpenSeaV1DataV1Dto.Side.SELL,
        saleKind = OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION,
        howToCall = OrderOpenSeaV1DataV1Dto.HowToCall.CALL,
        callData = randomBinary(),
        replacementPattern = randomBinary(),
        staticTarget = randomAddress(),
        staticExtraData = randomBinary(),
        extra = randomBigInt()
    )
}