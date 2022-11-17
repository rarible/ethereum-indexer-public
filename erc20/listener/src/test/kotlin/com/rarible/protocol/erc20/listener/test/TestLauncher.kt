package com.rarible.protocol.erc20.listener.test

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestLauncher : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking<Unit> {
        // We found if one of the descriptor's tests starts first, it becomes "broken",
        // most likely it related to the Kafka consumer used by Blockchain-Scanner

        // We give delay here to ensure these consumers are ready to work, but ideally,
        // there should be a mechanism allows to identify the readiness of scanner's components
        // TODO remove after Scanner V2
        delay(5000)
    }
}