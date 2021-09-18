package com.rarible.protocol.order.core.service.nft

import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class NftItemApiService(
    private val nftItemApi: NftItemControllerApi
) {
    suspend fun getNftItemById(itemId: String): NftItemDto? {
        return clientRequest {  nftItemApi.getNftItemById(itemId, false) }
    }

    suspend fun getNftLazyItemById(itemId: String): LazyNftDto? {
        return clientRequest {  nftItemApi.getNftLazyItemById(itemId) }
    }

    private suspend fun <T> clientRequest(body: suspend () -> Mono<T>): T? {
        return try {
            body().awaitFirstOrNull()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
    }
}