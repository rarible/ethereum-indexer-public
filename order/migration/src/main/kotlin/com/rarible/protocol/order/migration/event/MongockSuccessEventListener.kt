package com.rarible.protocol.order.migration.event

import com.github.cloudyrock.spring.util.events.SpringMigrationSuccessEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class MongockSuccessEventListener : ApplicationListener<SpringMigrationSuccessEvent> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: SpringMigrationSuccessEvent) {
        logger.info("Migration was finished successfully : ${event.timestamp}")
        exitProcess(0)
    }
}
