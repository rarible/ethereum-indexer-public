package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import scalether.domain.Address
import java.math.BigDecimal

data class AggregatedData(
    @Id
    val address: Address,
    val sum: BigDecimal,
    val count: Long
)
