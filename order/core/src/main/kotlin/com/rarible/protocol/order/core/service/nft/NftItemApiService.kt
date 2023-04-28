package com.rarible.protocol.order.core.service.nft

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
@CaptureSpan(type = SpanType.EXT)
class NftItemApiService(
    private val nftItemApi: NftItemControllerApi
) {

    suspend fun getNftItemById(itemId: String): NftItemDto? {
        return clientRequest { nftItemApi.getNftItemById(itemId).awaitFirstOrNull() }
    }

    suspend fun getNftLazyItemById(itemId: String): LazyNftDto? {
        return clientRequest { nftItemApi.getNftLazyItemById(itemId).awaitFirstOrNull() }
    }

    private suspend fun <T> clientRequest(body: suspend () -> T): T? {
        return try {
            body()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
    }
}
