package com.rarible.protocol.block.scanner.test

import io.daonomic.rpc.mono.WebClientTransport
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller

@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        return MonoEthereum(WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000))
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }
}
