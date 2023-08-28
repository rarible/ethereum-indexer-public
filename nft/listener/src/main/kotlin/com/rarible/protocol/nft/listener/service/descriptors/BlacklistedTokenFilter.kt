package com.rarible.protocol.nft.listener.service.descriptors

import com.rarible.blockchain.scanner.consumer.LogRecordFilter
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecordEvent
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import org.springframework.stereotype.Component

@Component
class BlacklistedTokenFilter(
    ignoredTokenResolver: IgnoredTokenResolver
) : LogRecordFilter<EthereumLogRecordEvent> {

    val ignoredTokens = ignoredTokenResolver.resolve()

    override fun filter(event: EthereumLogRecordEvent): Boolean {
        return ignoredTokens.contains(event.record.address).not()
    }
}
