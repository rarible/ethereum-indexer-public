package com.rarible.protocol.unlockable.api.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.unlockable.api.subscriber.UnlockableEventsConsumerFactory
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
        "protocol.unlockable.subscriber.broker-replicaset=PLAINTEXT://localhost:9092",
        "protocol.unlockable.subscriber.blockchain=ethereum"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(UnlockableEventsSubscriberAutoConfigurationIt.Configuration::class)
class UnlockableEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var unlockableEventsConsumerFactory: UnlockableEventsConsumerFactory

    @Test
    fun `test default consumer initialized`() {
        Assertions.assertThat(unlockableEventsConsumerFactory)
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
