package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
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

fun createNftOwnershipUpdateEvent(ownership: NftOwnershipDto): NftOwnershipUpdateEventDto {
    return NftOwnershipUpdateEventDto(
        eventId = randomString(),
        ownershipId = ownership.id,
        ownership = ownership
    )
}

fun createNftOwnershipDeleteEvent(ownership: NftOwnershipDto): NftOwnershipDeleteEventDto {
    return NftOwnershipDeleteEventDto(
        eventId = randomString(),
        ownershipId = ownership.id,
        ownership = null,
        deletedOwnership = ownership
    )
}

fun createNftOwnershipDeleteEventLegacy(ownership: NftOwnershipDto): NftOwnershipDeleteEventDto {
    val targetToken = ownership.contract
    val targetTokenId = EthUInt256.of(ownership.tokenId)

    val oldOwner = ownership.owner

    val deletedOwnership = NftDeletedOwnershipDto(
        id = randomString(),
        token = targetToken,
        tokenId = targetTokenId.value,
        owner = oldOwner
    )
    return NftOwnershipDeleteEventDto(
        eventId = randomString(),
        ownershipId = ownership.id,
        ownership = deletedOwnership,
        deletedOwnership = null
    )
}