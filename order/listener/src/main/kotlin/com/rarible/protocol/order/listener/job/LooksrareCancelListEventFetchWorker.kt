package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.service.looksrare.LooksrareCancelListEventLoadHandler
import io.micrometer.core.instrument.MeterRegistry

class LooksrareCancelListEventFetchWorker(
    private val handler: LooksrareCancelListEventLoadHandler,
    properties: LooksrareLoadProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "looksrare-cancel-list-event-load-job"
) {
    override suspend fun handle() {
        try {
            handler.handle()
        } catch (ex: Throwable) {
            throw RuntimeException(ex)
        }
    }
}
