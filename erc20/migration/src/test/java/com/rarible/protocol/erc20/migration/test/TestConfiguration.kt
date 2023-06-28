package com.rarible.protocol.erc20.migration.test

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.core.MonoEthereum

@TestConfiguration
class TestConfiguration {

    @Bean
    fun testEthereum(): MonoEthereum = mockk()
}
