package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.order.core.converters.dto.AssetTypeDtoConverter
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address
import java.math.BigInteger

@RestController
class OrderCurrenciesController(
    private val orderRepository: OrderRepository,
    private val assetTypeDtoConverter: AssetTypeDtoConverter
) : OrderCurrenciesControllerApi {
    override suspend fun bySellOrdersOfItem(
        contract: String,
        tokenId: String
    ): ResponseEntity<OrderCurrenciesDto> {
        val currencies = orderRepository.findTakeTypesOfSellOrders(
            Address.apply(contract),
            EthUInt256.of(BigInteger(tokenId))
        ).map { assetTypeDtoConverter.convert(it) }.toList()
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.SELL, currencies))
    }

    override suspend fun byBidOrdersOfItem(
        contract: String,
        tokenId: String
    ): ResponseEntity<OrderCurrenciesDto> {
        val currencies = orderRepository.findMakeTypesOfBidOrders(
            Address.apply(contract),
            EthUInt256.of(BigInteger(tokenId))
        ).map { assetTypeDtoConverter.convert(it) }.toList()
        return ResponseEntity.ok(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.BID, currencies))
    }

}