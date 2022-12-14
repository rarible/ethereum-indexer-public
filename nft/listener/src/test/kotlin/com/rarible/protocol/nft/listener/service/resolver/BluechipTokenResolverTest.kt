package com.rarible.protocol.nft.listener.service.resolver

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BluechipTokenResolverTest {
    private val properties = mockk<NftIndexerProperties>()
    private val appEnv = mockk<ApplicationEnvironmentInfo>()

    @Test
    fun `resolve - ok`() {
        every { properties.blockchain } returns Blockchain.ETHEREUM
        every { appEnv.name } returns "test"

        val resolver = BluechipTokenResolver(properties, appEnv)

        val resolved = resolver.resolve()
        assertThat(resolved).hasSize(1)
    }
}
