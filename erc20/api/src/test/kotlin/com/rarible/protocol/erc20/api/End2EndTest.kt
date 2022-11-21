package com.rarible.protocol.erc20.api

import com.rarible.core.test.ext.EthereumTest
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@Retention
@MongoTest
@EthereumTest
@AutoConfigureJson
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = e2e",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "common.blockchain = ethereum",
        "common.kafkaReplicaSet = ",
        "spring.cloud.discovery.enabled = false",
        "spring.cloud.consul.config.enabled = false",
        "logging.logjson.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("e2e", "ethereum")
annotation class End2EndTest
