package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.listener.configuration.StartEndWorkerProperties
import com.rarible.protocol.order.listener.service.order.OrderStartEndCheckerHandler
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.time.delay

@ExperimentalCoroutinesApi
class OrderStartEndCheckerWorker(
    private val handler: OrderStartEndCheckerHandler,
    properties: StartEndWorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "start-end-orders-check-job"
) {
    override suspend fun handle() {
        try {
            handler.handle()
            delay(pollingPeriod)
        } catch (ex: Throwable) {
            throw RuntimeException(ex)
        }
    }
}
