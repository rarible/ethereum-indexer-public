package com.rarible.protocol.nft.api.e2e

import com.rarible.core.cache.CacheService
import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaResolver
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import com.rarible.protocol.nft.core.service.token.meta.descriptors.OpenseaTokenPropertiesResolver
import com.rarible.protocol.nft.core.service.token.meta.descriptors.StandardTokenPropertiesResolver
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
    @Qualifier("mockItemMetaResolver")
    fun mockItemMetaResolver(): ItemMetaResolver = mockk()

    @Bean
    @Primary
    @Qualifier("mockStandardTokenPropertiesResolver")
    fun mockStandardTokenPropertiesResolver(): StandardTokenPropertiesResolver = mockk {
        every { order } returns -1
    }

    @Bean
    @Primary
    @Qualifier("mockOpenseaTokenPropertiesResolver")
    fun mockOpenseaTokenPropertiesResolver(): OpenseaTokenPropertiesResolver = mockk {
        every { order } returns 1
    }

    @Bean
    @Primary
    fun testTokenPropertiesService(
        cacheService: CacheService,
        @Qualifier("mockStandardTokenPropertiesResolver") standardPropertiesResolver: StandardTokenPropertiesResolver,
        @Qualifier("mockOpenseaTokenPropertiesResolver") openseaPropertiesResolver: OpenseaTokenPropertiesResolver
    ) : TokenPropertiesService {
        return TokenPropertiesService(Long.MAX_VALUE, cacheService, listOf(standardPropertiesResolver, openseaPropertiesResolver))
    }

    @Bean
    @Primary
    @Qualifier("mockMediaMetaService")
    fun mockMediaMetaService(): MediaMetaService = mockk()

}
