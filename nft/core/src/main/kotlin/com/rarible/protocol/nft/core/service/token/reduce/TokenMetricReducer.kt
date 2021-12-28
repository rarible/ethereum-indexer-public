package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.blockchain.scanner.ethereum.configuration.EthereumScannerProperties
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.AbstractMetricReducer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class TokenMetricReducer(
    properties: EthereumScannerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<TokenEvent, Token>(properties, meterRegistry, "token") {

    override fun getMetricName(event: TokenEvent): String {
        return when (event) {
            is TokenEvent.TokenChangeOwnershipEvent -> "change_ownership"
            is TokenEvent.TokenCreateEvent -> "create"
        }
    }
}
