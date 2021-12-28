package com.rarible.protocol.order.api.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftOwnershipDto
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

fun createNftOwnershipDto(): NftOwnershipDto {
    val token = AddressFactory.create()
    val tokenId = BigInteger.ONE
    return NftOwnershipDto(
        id = "$token:$tokenId",
        contract = token,
        tokenId = tokenId,
        date = nowMillis(),
        value = EthUInt256.ONE.value,
        owner = AddressFactory.create(),
        creators = emptyList(),
        pending = emptyList(),
        lazyValue = BigInteger.ZERO
    )
}

fun createNftItemDto(token: Address = AddressFactory.create(), tokenId: BigInteger = BigInteger.ONE): NftItemDto {
    return NftItemDto(
        id = "${token}:${tokenId}",
        contract = token,
        tokenId = tokenId,
        creators = emptyList(),
        lazySupply = BigInteger.ZERO,
        owners = emptyList(),
        royalties = emptyList(),
        supply = BigInteger.ONE,
        deleted = false,
        lastUpdatedAt = nowMillis(),
        pending = emptyList(),
        meta = NftItemMetaDto("Test", null, null, null, null)
    )
}

fun createNftCollectionDto(id: Address): NftCollectionDto {
    return NftCollectionDto(
        id = id,
        type = NftCollectionDto.Type.ERC721,
        features = listOf(NftCollectionDto.Features.MINT_AND_TRANSFER),
        name = "TEST",
        symbol = null,
        owner = null,
        supportsLazyMint = true,
        minters = emptyList()
    )
}
