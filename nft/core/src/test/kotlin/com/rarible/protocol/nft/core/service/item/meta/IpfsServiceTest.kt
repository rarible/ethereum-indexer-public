package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.client.DefaultProtocolWebClientCustomizer
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@ItemMetaTest
class IpfsServiceTest {
    private val service = IpfsService()

    @Test
    fun testRealUrl() {
        val pairs = listOf(
            "https://ipfs.io/ipfs/https://ipfs.io/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW" to "${IpfsService.RARIBLE_IPFS}/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW",
            "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw" to "${IpfsService.RARIBLE_IPFS}/ipfs/QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw",
            "ipfs://ipfs/QmaMTrfaPkHrD3RsoN7VECBn8Wea6pBg175GCWFNbQRK6R/cusses/some URL with space.gif" to "${IpfsService.RARIBLE_IPFS}/ipfs/QmaMTrfaPkHrD3RsoN7VECBn8Wea6pBg175GCWFNbQRK6R/cusses/some%20URL%20with%20space.gif"
        )
        for ((input, output) in pairs) {
            assertThat(service.resolveHttpUrl(input)).isEqualTo(output)
        }
    }

    @Disabled
    @Test
    fun upload() = runBlocking<Unit> {
        val byteArray = randomBigInt().toByteArray()
        val url = service.upload("testFile", byteArray, "application/octet-stream")
        val client = WebClient.builder().apply {
            DefaultProtocolWebClientCustomizer().customize(it)
        }.build()
        val receivedBytes = client.get().uri(url)
            .retrieve()
            .bodyToMono<ByteArray>()
            .awaitFirst()
        assertThat(receivedBytes).isEqualTo(byteArray)
    }
}
