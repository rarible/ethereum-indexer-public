package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDto
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