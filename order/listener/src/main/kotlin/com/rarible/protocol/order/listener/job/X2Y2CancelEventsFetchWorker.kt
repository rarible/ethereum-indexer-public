package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.listener.configuration.X2Y2EventLoadProperties
import com.rarible.protocol.order.listener.service.x2y2.X2Y2CancelEventsLoadHandler
import io.micrometer.core.instrument.MeterRegistry

class X2Y2CancelEventsFetchWorker(
    private val handler: X2Y2CancelEventsLoadHandler,
    properties: X2Y2EventLoadProperties,
    meterRegistry: MeterRegistry
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "x2y2-cancel-list-event-load-job"
) {
    override suspend fun handle() {
        try {
            handler.handle()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
