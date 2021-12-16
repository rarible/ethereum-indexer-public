package com.rarible.protocol.unlockable.test

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.PartDto
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.util.*

object NftItemDtoFactory {

    fun randomItemDto(id: String, address: Address) = NftItemDto(
        id = id,
        contract = AddressFactory.create(),
        tokenId = randomBigInt(),
        creators = listOf(PartDto(AddressFactory.create(), Random().nextInt())),
        supply = randomBigInt(),
        lazySupply = randomBigInt(),
        owners = listOf(address),
        royalties = emptyList(),
        pending = emptyList(),
        lastUpdatedAt = nowMillis(),
        deleted = false,
        meta = NftItemMetaDto("Test", null, null, null, null)
    )

}
