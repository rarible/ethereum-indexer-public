package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import com.rarible.protocol.order.listener.misc.LambdaList
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ContractsProvider(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val currencyContractAddresses: OrderIndexerProperties.CurrencyContractAddresses,
    private val sudoSwapAddresses: SudoSwapAddresses,
) {
    fun raribleExchangeV1(): List<Address> {
        return LambdaList { listOfNotNull(exchangeContractAddresses.v1, exchangeContractAddresses.v1Old) }
    }

    fun raribleExchangeV2(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.v2) }
    }

    fun cryptoPunks(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.cryptoPunks) }
    }

    fun looksrareV1(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.looksrareV1) }
    }

    fun seaportV1(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.seaportV1) }
    }

    fun openSeaV2(): Address {
        return exchangeContractAddresses.openSeaV2
    }

    fun openSea(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.openSeaV1, openSeaV2()) }
    }

    fun x2y2V1(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.x2y2V1) }
    }

    fun weth(): Address {
        return currencyContractAddresses.weth
    }

    fun pairFactoryV1(): List<Address> {
        return listOf(sudoSwapAddresses.pairFactoryV1)
    }

    fun linearCurveV1(): Address {
        return sudoSwapAddresses.linearCurveV1
    }

    fun exponentialCurveV1(): Address {
        return sudoSwapAddresses.exponentialCurveV1
    }
}