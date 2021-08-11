package com.rarible.protocol.order.core.misc

import com.rarible.protocol.order.core.model.Currency
import java.math.BigDecimal
import java.math.BigInteger

fun Currency?.toCents(value: BigDecimal): BigInteger = this?.toCents(value) ?: value.toBigInteger()
fun Currency?.fromCents(value: BigInteger): BigDecimal = this?.fromCents(value) ?: value.toBigDecimal()
fun Currency?.toEth(value: BigDecimal): BigDecimal? = this?.toEth(value)
