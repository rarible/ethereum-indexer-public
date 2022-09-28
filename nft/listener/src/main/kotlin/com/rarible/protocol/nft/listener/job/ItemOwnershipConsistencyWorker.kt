package com.rarible.protocol.nft.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.nft.listener.configuration.ItemOwnershipConsistencyProperties
import io.micrometer.core.instrument.MeterRegistry

const val ITEM_OWNERSHIP_CONSISTENCY_JOB = "item-ownership-consistency-job"

class ItemOwnershipConsistencyWorker(
    properties: ItemOwnershipConsistencyProperties,
    meterRegistry: MeterRegistry,
): SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = ITEM_OWNERSHIP_CONSISTENCY_JOB
) {


    override suspend fun handle() {
        logger.info("Handle item ownership consistency job started")
        // TODO implement
    }
}
