package com.rarible.protocol.nft.listener.service.resolver

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class IgnoredTokenResolverTest {
    private val listenerProperties = mockk<NftListenerProperties>()
    private val properties = mockk<NftIndexerProperties>()
    private val appEnv = mockk<ApplicationEnvironmentInfo>()

    @Test
    fun `resolve - ok, only from config`() {
        val fromConfig = setOf(randomAddress(), randomAddress())
        every { properties.blockchain } returns Blockchain.ETHEREUM
        every { listenerProperties.skipTransferContracts } returns fromConfig.map { it.prefixed() }
        every { appEnv.name } returns randomString()

        val ignoredTokenResolver = IgnoredTokenResolver(listenerProperties, properties, appEnv)

        val resolved = ignoredTokenResolver.resolve()
        assertThat(resolved).isEqualTo(fromConfig)
    }

    @Test
    fun `resolve - ok, from file and config`() {
        val fromConfig = setOf(randomAddress(), randomAddress())
        every { properties.blockchain } returns Blockchain.ETHEREUM
        every { listenerProperties.skipTransferContracts } returns fromConfig.map { it.prefixed() }
        every { appEnv.name } returns "test"

        val ignoredTokenResolver = IgnoredTokenResolver(listenerProperties, properties, appEnv)

        val resolved = ignoredTokenResolver.resolve()
        assertThat(resolved).hasSize(4)
    }
}
