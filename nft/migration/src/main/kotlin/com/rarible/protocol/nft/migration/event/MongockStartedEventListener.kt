package com.rarible.protocol.nft.migration.event

import com.github.cloudyrock.spring.util.events.SpringMigrationStartedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class MongockStartedEventListener : ApplicationListener<SpringMigrationStartedEvent> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: SpringMigrationStartedEvent) {
        logger.info("Start migration: ${event.timestamp}")
    }
}
