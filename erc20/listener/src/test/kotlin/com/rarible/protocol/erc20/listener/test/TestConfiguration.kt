package com.rarible.protocol.erc20.listener.test

import com.rarible.ethereum.cache.CacheableMonoEthereum
import com.rarible.protocol.erc20.listener.consumer.KafkaEntityEventConsumer
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionPoller
import java.time.Duration

@TestConfiguration
@ComponentScan(basePackageClasses = [IntegrationTest::class])
class TestConfiguration {

    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        val transport = WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000)
        return CacheableMonoEthereum(
            transport = transport,
            expireAfter = Duration.ofMinutes(1),
            cacheMaxSize = 100
        )
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    @Primary
    fun testEntityEventConsumer(): KafkaEntityEventConsumer = mockk()
}
