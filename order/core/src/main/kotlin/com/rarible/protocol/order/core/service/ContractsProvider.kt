package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.configuration.SudoSwapAddresses
import com.rarible.protocol.order.core.misc.LambdaList
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ContractsProvider(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val auctionContractAddresses: OrderIndexerProperties.AuctionContractAddresses,
    private val currencyContractAddresses: OrderIndexerProperties.CurrencyContractAddresses,
    private val sudoSwapAddresses: SudoSwapAddresses,
) {

    fun raribleExchangeV1(): List<Address> {
        return LambdaList { listOfNotNull(exchangeContractAddresses.v1, exchangeContractAddresses.v1Old) }
    }

    fun raribleExchangeV2(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.v2) }
    }

    fun raribleAuctionV1(): List<Address> {
        return LambdaList { listOf(auctionContractAddresses.v1) }
    }

    fun cryptoPunks(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.cryptoPunks) }
    }

    fun looksrareV1(): List<Address> {
        return listOf(exchangeContractAddresses.looksrareV1)
    }

    fun looksrareV2(): List<Address> {
        return listOf(exchangeContractAddresses.looksrareV2)
    }

    fun seaportV1(): List<Address> {
        return listOf(
            exchangeContractAddresses.seaportV1,
            exchangeContractAddresses.seaportV1_4,
            exchangeContractAddresses.seaportV1_5
        )
    }

    fun blurV1(): List<Address> {
        return listOf(exchangeContractAddresses.blurV1)
    }

    fun blurV2(): List<Address> {
        return listOf(exchangeContractAddresses.blurV2)
    }

    fun openSeaV2(): Address {
        return exchangeContractAddresses.openSeaV2
    }

    fun openSea(): List<Address> {
        return LambdaList { listOf(exchangeContractAddresses.openSeaV1, openSeaV2()) }
    }

    fun x2y2V1(): List<Address> {
        return listOf(exchangeContractAddresses.x2y2V1)
    }

    fun zeroEx(): List<Address> {
        return listOf(exchangeContractAddresses.zeroEx)
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