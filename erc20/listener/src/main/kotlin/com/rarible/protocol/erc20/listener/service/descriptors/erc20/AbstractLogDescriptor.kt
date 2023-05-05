package com.rarible.protocol.erc20.listener.service.descriptors.erc20

import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.listener.service.owners.IgnoredOwnersResolver

abstract class AbstractLogDescriptor(
    ignoredOwnersResolver: IgnoredOwnersResolver,
    private val metrics: DescriptorMetrics
) {

    private val ignoredOwners = ignoredOwnersResolver.resolve()

    protected fun Iterable<Erc20TokenHistory>.filterByOwner() =
        filterTo(ArrayList()) { log ->
            metrics.onIncoming()
            val ignored = log.owner in ignoredOwners
            if (ignored) {
                metrics.onIgnored()
            } else {
                metrics.onSaved()
            }
            !ignored
        }
}
