package com.rarible.protocol.order.core.repository.currency

import com.rarible.protocol.order.core.model.Currency
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import scalether.domain.Address

class CurrencyRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun findFirstByAddress(address: Address): Currency? {
        return template.findById<Currency>(address).awaitFirstOrNull()
    }
}