package com.rarible.protocol.nft.core.service.item.meta

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IpfsServiceTest {
    private val service = IpfsService("https://rarible.mypinata.cloud", mockk())

    @Test
    fun testRealUrl() {
        val url = service.resolveRealUrl("https://ipfs.io/ipfs/https://ipfs.io/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")
        assertThat(url)
            .isEqualTo("https://rarible.mypinata.cloud/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")
    }
}
