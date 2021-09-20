package com.rarible.protocol.nftorder.listener.test.mock.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import java.math.BigInteger

fun randomEthUInt256() = EthUInt256.of(randomWord())

fun randomPart() = Part(randomAddress(), randomInt())
fun randomPartDto() = randomPartDto(randomAddress())
fun randomPartDto(account: Address) = PartDto(account, randomInt())

fun randomItemId() = ItemId(randomAddress(), randomEthUInt256())
fun randomOwnershipId(itemId: ItemId) = OwnershipId(itemId.token, itemId.tokenId, randomAddress())
fun randomOwnershipId() = OwnershipId(randomAddress(), randomEthUInt256(), randomAddress())

fun randomItem(itemId: ItemId) = randomItem(itemId, randomPart())
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
        meta = NftItemMetaDto(randomString(), randomString(), listOf(NftItemAttributeDto(randomString(), randomString())), null, null)
    )
}


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

fun randomNftOwnershipDto() = randomNftOwnershipDto(randomOwnershipId())
fun randomNftOwnershipDto(itemId: ItemId) = randomNftOwnershipDto(itemId, randomPartDto())
fun randomNftOwnershipDto(ownershipId: OwnershipId) = randomNftOwnershipDto(
    ItemId(ownershipId.token, ownershipId.tokenId),
    PartDto(ownershipId.owner, randomInt())
)

fun randomNftOwnershipDto(itemId: ItemId, creator: PartDto): NftOwnershipDto {
    val ownershipId = OwnershipId(itemId.token, itemId.tokenId, creator.account)
    return NftOwnershipDto(
        id = ownershipId.decimalStringValue,
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
fun randomAssetErc1155() = randomAssetErc721(randomItemId())

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

fun randomNftItemMetaDto(): NftItemMetaDto {
    return NftItemMetaDto(
        name = randomString(),
        attributes = listOf(randomNftItemAttributeDto(), randomNftItemAttributeDto()),
        description = randomString(),
        animation = NftMediaDto(
            url = mapOf(randomString() to randomString()),
            meta = mapOf(randomString() to randomNftMediaMetaDto())
        ),
        image = NftMediaDto(
            url = mapOf(randomString() to randomString()),
            meta = mapOf(randomString() to randomNftMediaMetaDto())
        )
    )
}

fun randomNftItemAttributeDto() = NftItemAttributeDto(randomString(), randomString())

fun randomNftMediaMetaDto(): NftMediaMetaDto {
    return NftMediaMetaDto(
        height = randomInt(),
        width = randomInt(),
        type = randomString()
    )
}

fun randomLazyErc721Dto(itemId: ItemId): LazyNftDto {
    return LazyErc721Dto(
        contract = itemId.token,
        tokenId = itemId.tokenId.value,
        uri = randomString(),
        creators = listOf(randomPartDto()),
        royalties = listOf(randomPartDto()),
        signatures = listOf(Binary.apply())
    )
}

fun randomNftSignatureDto(): NftSignatureDto {
    return NftSignatureDto(
        v = RandomUtils.nextBytes(1)[0],
        r = randomBinary(),
        s = randomBinary()
    )
}

fun randomNftCollectionDto() = randomNftCollectionDto(randomAddress())
fun randomNftCollectionDto(id: Address): NftCollectionDto {
    return NftCollectionDto(
        id = id,
        type = NftCollectionDto.Type.ERC1155,
        owner = randomAddress(),
        name = randomString(),
        symbol = randomString(),
        features = listOf(NftCollectionDto.Features.MINT_WITH_ADDRESS, NftCollectionDto.Features.APPROVE_FOR_ALL),
        supportsLazyMint = true
    )
}
