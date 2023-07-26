package com.rarible.protocol.order.core.service.nft

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.EXT)
class NftCollectionApiService(
    private val nftCollectionApi: NftCollectionControllerApi
) {
    suspend fun getNftCollectionById(collectionId: Address): NftCollectionDto? {
        return try {
            nftCollectionApi.getNftCollectionById(collectionId.prefixed()).awaitFirstOrNull()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
    }
}
