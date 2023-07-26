package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.misc.safeDivide128
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

@Document("currency")
data class Currency(
    @Id
    val address: Address,

    @Indexed(background = true)
    val symbol: String,

    val name: String,

    val decimals: Int,

    val rate: BigDecimal
) {
    fun toCents(value: BigDecimal): BigInteger = (value * BigDecimal.TEN.pow(decimals)).toBigInteger()

    fun fromCents(value: BigInteger): BigDecimal = value.toBigDecimal().safeDivide128(BigDecimal.TEN.pow(decimals))

    fun toEth(value: BigDecimal): BigDecimal = rate * value

    companion object {
        val ETH = Currency(Address.ZERO(), "ETH", "Ether", 18, BigDecimal.ONE)
        val ETH_TOKEN: Address = ETH.address
    }
}
