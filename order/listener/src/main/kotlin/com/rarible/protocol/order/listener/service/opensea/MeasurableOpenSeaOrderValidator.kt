package com.rarible.protocol.order.listener.service.opensea

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.OrderVersion
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class MeasurableOpenSeaOrderValidator(
    private val delegate:OpenSeaOrderValidatorImp,
    private val micrometer: MeterRegistry,
    private val blockchain: Blockchain
) :OpenSeaOrderValidator{

    private val counter = Counter.builder("protocol.opensea.order.validator.error")
        .tag("blockchain", blockchain.value)
        .register(micrometer)

    override fun validate(order: OrderVersion): Boolean {
        val result = delegate.validate(order)

        if (!result) {
            counter.increment()
        }
        return result
    }
}