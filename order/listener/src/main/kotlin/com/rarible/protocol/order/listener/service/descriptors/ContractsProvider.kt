package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.listener.misc.LambdaList
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ContractsProvider(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
) {
    fun raribleExchangeV1(): List<Address> {
        return LambdaList { listOfNotNull(exchangeContractAddresses.v1, exchangeContractAddresses.v1Old) }
    }

    fun raribleExchangeV2(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.v2) }
    }
}