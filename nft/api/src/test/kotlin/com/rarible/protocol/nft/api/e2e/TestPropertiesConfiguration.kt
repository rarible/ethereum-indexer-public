package com.rarible.protocol.nft.api.e2e

import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolverProvider
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    @Primary
    fun mockLazyNftValidator(): LazyNftValidator = mockk()

    @Bean
    @Primary
    @Qualifier("mockItemPropertiesResolver")
    fun mockItemPropertiesResolver(): ItemPropertiesResolver = mockk {
        every { name } returns "MockResolver"
        every { canBeCached } returns true
    }

    @Bean
    @Primary
    fun mockItemPropertiesResolverProvider(
        @Qualifier("mockItemPropertiesResolver") mockItemPropertiesResolver: ItemPropertiesResolver
    ): ItemPropertiesResolverProvider = mockk {
        every { orderedResolvers } returns listOf(mockItemPropertiesResolver)
    }
}
