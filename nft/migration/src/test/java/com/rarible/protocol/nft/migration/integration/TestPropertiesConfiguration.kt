package com.rarible.protocol.nft.migration.integration

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.transaction.MonoTransactionSender

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun transactionSender(): MonoTransactionSender {
        return mockk()
    }
}
