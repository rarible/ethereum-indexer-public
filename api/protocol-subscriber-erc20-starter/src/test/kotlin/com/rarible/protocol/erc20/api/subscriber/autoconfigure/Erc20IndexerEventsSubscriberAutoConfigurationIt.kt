package com.rarible.protocol.erc20.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.erc20.api.subscriber.Erc20IndexerEventsConsumerFactory
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
        "protocol.erc20.subscriber.broker-replicaset=PLAINTEXT://localhost:9092",
        "protocol.erc20.subscriber.blockchain=ethereum"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(Erc20IndexerEventsSubscriberAutoConfigurationIt.Configuration::class)
class Erc20IndexerEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var erc20IndexerEventsConsumerFactory: Erc20IndexerEventsConsumerFactory

    @Test
    fun `test default consumer initialized`() {
        Assertions.assertThat(erc20IndexerEventsConsumerFactory).isNotNull
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
