package com.rarible.protocol.order.listener.service.converter

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.trace.TraceCallService
import io.daonomic.rpc.domain.Binary
import scalether.domain.response.Log
import scalether.domain.response.Transaction

abstract class AbstractEventConverter(
    protected val traceCallService: TraceCallService,
    protected val featureFlags: OrderIndexerProperties.FeatureFlags,
) {
    protected open suspend fun getMethodInput(log: Log, transaction: Transaction, vararg methodId: Binary): List<Binary> {
        return if (transaction.input().methodSignatureId() in methodId) {
            listOf(transaction.input())
        } else {
            if (featureFlags.skipGetTrace) {
                emptyList()
            } else {
                traceCallService.findAllRequiredCallInputs(
                    txHash = transaction.hash(),
                    txInput = transaction.input(),
                    to = log.address(),
                    ids = methodId
                )
            }
        }
    }
}
