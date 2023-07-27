package com.rarible.protocol.order.migration.event

import com.github.cloudyrock.spring.util.events.SpringMigrationFailureEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class MongockFailEventListener : ApplicationListener<SpringMigrationFailureEvent> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: SpringMigrationFailureEvent) {
        logger.error("Migration was finished with failure : ${event.exception}")
        exitProcess(-1)
    }
}
