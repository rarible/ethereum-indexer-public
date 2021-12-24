package com.rarible.protocol.nft.listener.integration

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainClient
import com.rarible.protocol.nft.listener.test.TestEthereumBlockchainClient
import io.daonomic.rpc.mono.WebClientTransport
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
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

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["common.feature-flags.scanner-version"], havingValue = "V2")
    fun testEthereumBlockchainClient(
        blockchainClient: EthereumBlockchainClient
    ): EthereumBlockchainClient {
        return TestEthereumBlockchainClient(blockchainClient)
    }
}
