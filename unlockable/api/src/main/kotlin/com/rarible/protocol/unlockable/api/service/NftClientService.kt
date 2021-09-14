package com.rarible.protocol.unlockable.api.service

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

@Component
class NftClientService(
    val nftItemControllerApi: NftItemControllerApi,
    val nftOwnershipControllerApi: NftOwnershipControllerApi
) {

    suspend fun getItem(id: String): NftItemDto? {
        return nftItemControllerApi.getNftItemById(id).awaitFirstOrNull()
    }

    suspend fun hasItem(contract: Address, tokenId: BigInteger, signerAddress: Address): Boolean {
        return nftOwnershipControllerApi
            .getNftOwnershipsByItem(contract.hex(), tokenId.toString(), null, 1000)
            .awaitFirstOrDefault(NftOwnershipsDto(0, null, emptyList()))
            .ownerships
            .any { it.value != BigDecimal.ZERO }
    }

}

