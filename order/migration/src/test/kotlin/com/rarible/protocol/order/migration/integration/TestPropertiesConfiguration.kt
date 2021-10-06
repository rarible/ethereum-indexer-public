package com.rarible.protocol.order.migration.integration

import com.rarible.protocol.order.core.service.BlockProcessor
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.core.MonoEthereum
import scalether.transaction.MonoTransactionSender


@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun monoEthereum(): MonoEthereum {
        return MonoEthereum(WebClientTransport("https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/",
            MonoEthereum.mapper(), 10000, 10000))
    }
}
