package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.listener.configuration.X2Y2OrderLoadProperties
import com.rarible.protocol.order.listener.service.x2y2.X2Y2OrderLoadHandler
import io.micrometer.core.instrument.MeterRegistry

class X2Y2OrdersFetchWorker(
    private val handler: X2Y2OrderLoadHandler,
    properties: X2Y2OrderLoadProperties,
    meterRegistry: MeterRegistry
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "x2y2-orders-load-job"
) {
    override suspend fun handle() {
        try {
            handler.handle()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
