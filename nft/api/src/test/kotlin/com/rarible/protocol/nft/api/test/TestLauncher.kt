package com.rarible.protocol.nft.api.test

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestLauncher(
    private val kafkaConsumers: RaribleKafkaConsumerWorker<*>
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        logger.info("Test context started, launching test consumers")
        kafkaConsumers.start()
    }
}
