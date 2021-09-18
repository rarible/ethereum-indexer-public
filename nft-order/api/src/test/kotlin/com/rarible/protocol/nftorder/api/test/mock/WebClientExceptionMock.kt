package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.nft.api.ApiClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.StandardCharsets

object WebClientExceptionMock {

    private val mapper = ApiClient.createDefaultObjectMapper()

    fun mock(status: Int, data: Any): WebClientResponseProxyException {
        val original = WebClientResponseException(
            status,
            "",
            null,
            mapper.writeValueAsBytes(data),
            StandardCharsets.UTF_8
        )
        val result = WebClientResponseProxyException(original)
        result.data = data
        return result
    }

}