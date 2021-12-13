package com.rarible.protocol.nft.api.e2e

import com.rarible.core.test.ext.EthereumTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@Retention
@MongoTest
@KafkaTest
@EthereumTest
@RedisTest
@Testcontainers
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = e2e",
        "api.chain-id = 4",
        "api.opensea.url = https://api.opensea.io/api/v1",
        "api.opensea.api-key = test",
        "api.properties.cache-timeout = 1000",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "logging.level.org.springframework.web=DEBUG",
        "logging.level.org.springframework.data.mongodb.core.ReactiveMongoTemplate=DEBUG"
    ]
)
@ActiveProfiles("integration", "reduce-v2")
@Import(TestPropertiesConfiguration::class)
annotation class End2EndTest
