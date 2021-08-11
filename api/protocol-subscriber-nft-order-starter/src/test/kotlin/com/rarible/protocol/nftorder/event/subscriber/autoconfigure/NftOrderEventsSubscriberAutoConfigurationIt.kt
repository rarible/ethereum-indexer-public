package com.rarible.protocol.nftorder.event.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.nftorder.api.subscriber.NftOrderEventsConsumerFactory
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
        "protocol.nft-order.subscriber.broker-replicaset=PLAINTEXT://localhost:9092",
        "protocol.nft-order.subscriber.blockchain=ethereum"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(NftOrderEventsSubscriberAutoConfigurationIt.Configuration::class)
class NftOrderEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var nftIndexerEventsConsumerFactory: NftOrderEventsConsumerFactory

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
