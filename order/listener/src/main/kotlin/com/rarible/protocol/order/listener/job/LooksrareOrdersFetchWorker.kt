package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.service.looksrare.LooksrareOrderLoadHandler
import io.micrometer.core.instrument.MeterRegistry

class LooksrareOrdersFetchWorker(
    private val handler: LooksrareOrderLoadHandler,
    properties: LooksrareLoadProperties,
    meterRegistry: MeterRegistry,
): SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "seaport-orders-load-job"
) {
    override suspend fun handle() {
        try {
            handler.handle()
        } catch (ex: Throwable) {
            throw RuntimeException(ex)
        }
    }
}