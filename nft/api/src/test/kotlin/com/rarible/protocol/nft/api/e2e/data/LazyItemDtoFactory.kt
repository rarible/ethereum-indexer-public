package com.rarible.protocol.nft.api.e2e.data

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.PartDto
import io.daonomic.rpc.domain.Binary
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

fun createLazyErc721Dto(): LazyErc721Dto {
    val token = createAddress()
    val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))

    return LazyErc721Dto(
        contract = token,
        tokenId = tokenId.value,
        uri = UUID.randomUUID().toString(),
        royalties = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createPartDto() },
        creators = listOf(PartDto(AddressFactory.create(), 5000), PartDto(AddressFactory.create(), 5000)),
        signatures = listOf(Binary.empty(), Binary.empty())
    )
}

fun createLazyErc1155Dto(): LazyErc1155Dto {
    val token = createAddress()
    val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))

    return LazyErc1155Dto(
        contract = token,
        tokenId = tokenId.value,
        uri = UUID.randomUUID().toString(),
        royalties = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createPartDto() },
        creators = listOf(PartDto(AddressFactory.create(), 5000), PartDto(AddressFactory.create(), 5000)),
        supply = BigInteger.TEN,
        signatures = listOf(Binary.empty(), Binary.empty())
    )
}
