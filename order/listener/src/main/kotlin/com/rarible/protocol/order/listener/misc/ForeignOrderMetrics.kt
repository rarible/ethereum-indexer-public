package com.rarible.protocol.order.listener.misc

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.metric.BaseOrderMetrics
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class ForeignOrderMetrics(
    properties: OrderIndexerProperties,
    meterRegistry: MeterRegistry
) : BaseOrderMetrics(meterRegistry) {

    private val blockchain = properties.blockchain

    // Downloaded via API, successfully handled/saved
    fun onDownloadedOrderHandled(platform: Platform) {
        onDownloadedOrderHandled(platform, "ok", "ok")
    }

    // Downloaded via API, intentionally skipped (we don't need it)
    fun onDownloadedOrderSkipped(platform: Platform, reason: String) {
        onDownloadedOrderHandled(platform, "skipped", reason)
    }

    // Downloaded via API, contains incorrect data (ideally should be 0)
    fun onDownloadedOrderError(platform: Platform, reason: String) {
        onDownloadedOrderHandled(platform, "fail", reason)
    }

    // Received event (via blockchain or long-polling) successfully handled, data updated
    fun onOrderEventHandled(platform: Platform, type: String) {
        onOrderEventHandled(platform, type, "ok", "ok")
    }

    // Received event intentionally skipped (we don't need it)
    fun onOrderEventSkipped(platform: Platform, type: String, reason: String) {
        onOrderEventHandled(platform, type, "skipped", reason)
    }

    // Received event contains incorrect data (ideally should be 0)
    fun onOrderEventError(platform: Platform, type: String, reason: String) {
        onOrderEventHandled(platform, type, "fail", reason)
    }

    /**
     * Measure of "delay" between 'now' and order's date
     */
    fun onOrderReceived(
        platform: Platform,
        date: Instant,
        // replace if we download something else except orders to support
        // correct state of orders in our DB
        type: String = "order"
    ) {
        meterRegistry.timer(
            FOREIGN_ORDER_DOWNLOAD_DELAY,
            listOf(
                tag(blockchain),
                tag(platform),
                type(type.lowercase())
            )
        ).record(Duration.between(date, Instant.now()))
    }

    fun onLatestOrderReceived(
        platform: Platform,
        date: Instant,
        type: String = "order"
    ) {
        meterRegistry.gauge(
            FOREIGN_ORDER_DOWNLOAD_DELAY,
            listOf(
                tag(blockchain),
                tag(platform),
                type(type.lowercase())
            ),
            Duration.between(date, Instant.now()).seconds
        )
    }

    private fun onDownloadedOrderHandled(
        platform: Platform,
        status: String,
        reason: String
    ) {
        meterRegistry.counter(
            FOREIGN_ORDER_DOWNLOAD,
            listOf(
                tag(blockchain),
                tag(platform),
                status(status.lowercase()),
                reason(reason.lowercase())
            )
        ).increment()
    }

    private fun onOrderEventHandled(
        platform: Platform,
        type: String,
        status: String,
        reason: String
    ) {
        meterRegistry.counter(
            FOREIGN_ORDER_EVENT,
            listOf(
                tag(blockchain),
                tag(platform),
                type(type.lowercase()),
                status(status.lowercase()),
                reason(reason.lowercase())
            )
        ).increment()
    }

    fun onOrderInconsistency(
        platform: Platform,
        status: String,
    ) {
        meterRegistry.counter(
            FOREIGN_ORDER_INCONSISTENCY,
            listOf(
                tag(blockchain),
                tag(platform),
                status(status),
            )
        ).increment()
    }

    private companion object {

        const val FOREIGN_ORDER_DOWNLOAD = "foreign_order_download"
        const val FOREIGN_ORDER_DOWNLOAD_DELAY = "foreign_order_download_delay"
        const val FOREIGN_ORDER_EVENT = "foreign_order_event"
        const val FOREIGN_ORDER_INCONSISTENCY = "foreign_order_inconsistency"
    }
}