package com.rarible.protocol.order.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
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
        "protocol.order.subscriber.broker-replicaset=PLAINTEXT://localhost:9092",
        "protocol.order.subscriber.blockchain=ethereum"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(OrderIndexerEventsSubscriberAutoConfigurationIt.Configuration::class)
class OrderIndexerEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory

    @Test
    fun `test default consumer initialized`() {
        Assertions.assertThat(orderIndexerEventsConsumerFactory).isNotNull
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
