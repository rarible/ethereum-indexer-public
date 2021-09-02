package com.rarible.protocol.unlockable.util

import io.daonomic.rpc.domain.Binary
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import scalether.domain.Address
import scalether.util.Hex
import java.nio.charset.StandardCharsets
import java.security.SignatureException

object SignUtil {

    private val logger: Logger = LoggerFactory.getLogger(SignUtil::class.java)
    private const val START = "\u0019Ethereum Signed Message:\n"

    private fun fixV(v: Byte): Byte {
        return if (v < 27) (27 + v).toByte() else v
    }

    fun addStart(message: String): Binary {
        val string = if (message.startsWith("0x")) {
            String(Hex.toBytes(message), StandardCharsets.UTF_8)
        } else {
            message
        }
        val resultMessage = START + (string.toByteArray(StandardCharsets.UTF_8).size) + string
        return Binary.apply(resultMessage.toByteArray(StandardCharsets.UTF_8))
    }

    fun recover(message: String, signature: Binary?): Address {
        val data = recover(addStart(message).bytes(), signature)
        logger.info("recovered address: {}", data)
        return data
    }

    @Throws(SignatureException::class)
    fun recover(message: ByteArray, signature: Binary?): Address {
        if (signature == null) {
            throw SignatureException("Signature is null for message: $message")
        }
        val v = fixV(signature.bytes()[64])
        val r = ByteArray(32)
        val s = ByteArray(32)
        System.arraycopy(signature.bytes(), 0, r, 0, 32)
        System.arraycopy(signature.bytes(), 32, s, 0, 32)
        logger.info("recoverInternal r={} s={} v={}", Hex.to(r), Hex.to(s), v)
        val publicKey = Sign.signedMessageToKey(message, Sign.SignatureData(v, r, s))
        return Address.apply(Keys.getAddress(publicKey))
    }

}