package com.rarible.protocol.nft.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest(
    properties = [
        "protocol.nft.subscriber.broker-replicaset=PLAINTEXT://localhost:9092",
        "protocol.nft.subscriber.blockchain=ethereum"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(NftIndexerEventsSubscriberAutoConfigurationIt.Configuration::class)
class NftIndexerEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory

    @Test
    fun `test default consumer initialized`() {
        Assertions.assertThat(nftIndexerEventsConsumerFactory).isNotNull
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
