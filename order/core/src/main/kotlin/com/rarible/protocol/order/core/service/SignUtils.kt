package com.rarible.protocol.order.core.service

import io.daonomic.rpc.domain.Binary
import org.web3jold.crypto.Sign
import java.math.BigInteger
import java.nio.charset.StandardCharsets

class SignUtils {
    companion object {
        fun sign(privateKey: BigInteger, message: String): Sign.SignatureData {
            return Sign.signMessage(
                addStart(message).bytes(),
                Sign.publicKeyFromPrivate(privateKey),
                privateKey
            )
        }

        private fun addStart(message: String): Binary {
            val resultMessage = START + message.length + message
            return Binary.apply(resultMessage.toByteArray(StandardCharsets.US_ASCII))
        }

        private const val START = "\u0019Ethereum Signed Message:\n"
    }
}

data class SignedLong(
    val value: Long,
    val v: Byte,
    val r: Binary,
    val s: Binary
)

fun Byte.toEth(): BigInteger = BigInteger.valueOf(this.toLong())
