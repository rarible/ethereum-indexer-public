package com.rarible.protocol.nft.core.test

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestLauncher(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking<Unit> {
        logger.info("Test context started, launching test consumers")
        kafkaConsumers.forEach { it.start() }
    }
}
