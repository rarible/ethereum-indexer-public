package com.rarible.protocol.order.listener.service.currency

import com.rarible.protocol.order.core.model.Currency
import com.rarible.protocol.order.core.repository.currency.CurrencyRepository
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class CurrencyService(
    private val repository: CurrencyRepository
) {
    suspend fun byAddress(address: Address): Currency? {
        return if (address == Address.ZERO()) {
            Currency.ETH
        } else {
            repository.findFirstByAddress(address)
        }
    }
}