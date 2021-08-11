package com.rarible.protocol.nftorder.core.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class IntegrationTestConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    // In case when we have dedicated mocks, it's better to define them as beans instead of using
    // @MockkBean - that allow Spring to reuse launched context for different tests and, as a result,
    // gives significant speedup for test's run
    @Bean
    @Primary
    fun testNftItemControllerApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testNftOwnershipControllerApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testOrderControllerApi(): OrderControllerApi = mockk()

    @Bean
    @Primary
    fun testLockControllerApi(): LockControllerApi = mockk()
}