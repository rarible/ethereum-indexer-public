package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger

data class HeadTransaction(
    val hash: Word,
    val input: Binary,
    val from: Address,
    val to: Address?,
    val value: BigInteger
) {
    companion object {
        fun from(transaction: Transaction): HeadTransaction {
            return HeadTransaction(
                hash = transaction.hash(),
                from = transaction.from(),
                to = transaction.to(),
                input = transaction.input(),
                value = transaction.value()
            )
        }
    }
}
