package com.rarible.protocol.order.core.service.nft

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class NftOwnershipApiService(
    private val nftOwnershipControllerApi: NftOwnershipControllerApi
) {
    suspend fun getOwnershipById(ownershipId: String): NftOwnershipDto? {
        return try {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId, true).awaitFirstOrNull()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
    }
}

