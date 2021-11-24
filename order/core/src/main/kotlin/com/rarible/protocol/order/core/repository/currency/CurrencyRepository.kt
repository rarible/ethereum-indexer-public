package com.rarible.protocol.order.core.repository.currency

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.Currency
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import scalether.domain.Address

@CaptureSpan(type = SpanType.DB)
@Component
class CurrencyRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun findFirstByAddress(address: Address): Currency? {
        return template.findById<Currency>(address).awaitFirstOrNull()
    }
}
