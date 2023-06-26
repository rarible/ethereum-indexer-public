package com.rarible.protocol.order.core.service

import com.rarible.ethereum.common.keccak256
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import org.web3jold.crypto.Hash
import org.web3jold.crypto.Keys
import org.web3jold.crypto.Sign
import scalether.domain.Address
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

@Component
class CommonSigner {
    fun fixSignature(signature: Binary): Binary {
        if (signature.length() != 65) return signature

        val v = signature.bytes()[64]
        val fixedV = fixV(v)
        return if (fixedV == v) {
            signature
        } else {
            val byteArray = ByteArray(65)
            byteArray[64] = fixedV
            System.arraycopy(signature.bytes(), 0, byteArray, 0, 64)
            Binary(byteArray)
        }
    }

    fun recover(message: String, signature: Binary): Address {
        val v = fixV(signature.bytes()[64])
        val r = ByteArray(32)
        val s = ByteArray(32)
        System.arraycopy(signature.bytes(), 0, r, 0, 32)
        System.arraycopy(signature.bytes(), 32, s, 0, 32)
        return recover(message, Sign.SignatureData(v, r, s))
    }

    fun recover(message: String, signature: Sign.SignatureData): Address {
        val hash = hashToSign(message)
        val publicKey = Sign.signedMessageHashToKey(hash.bytes(), signature)
        return Address.apply(Keys.getAddress(publicKey))
    }

    fun recover(hash: Word, signature: Binary): Address {
        val r = ByteArray(32)
        val s = ByteArray(32)
        System.arraycopy(signature.bytes(), 0, r, 0, 32)
        System.arraycopy(signature.bytes(), 32, s, 0, 32)

        val v = if (signature.length() == 64) {
            val extractedV = getV(s)
            clearV(s)
            extractedV
        } else {
            signature.bytes()[64]
        }.let { fixV(it) }

        val publicKey = Sign.signedMessageHashToKey(hash.bytes(), Sign.SignatureData(v, r, s))
        return Address.apply(Keys.getAddress(publicKey))
    }

    fun hashToSign(message: String): Word {
        val withStart = addStart(message).bytes()
        return Word(Hash.sha3(withStart))
    }

    fun hashToSign(hash: Word): Word {
        val withStart = addStart(hash.hex()).bytes()
        return Word(Hash.sha3(withStart))
    }

    fun ethSignHashToSign(hash: Word): Word {
        return keccak256(Binary.apply("${START}32".toByteArray()).add(hash))
    }

    companion object {
        private const val START = "\u0019Ethereum Signed Message:\n"
        private const val V_BIT_POSITION = 0

        private fun getV(s: ByteArray): Byte {
            return if (s[V_BIT_POSITION].and(0x80.toByte()) == 0.toByte()) 0 else 1
        }

        private fun clearV(s: ByteArray): ByteArray {
             return s.apply { this[V_BIT_POSITION] = this[V_BIT_POSITION].and(0x7f.toByte()) }
        }

        private fun fixV(v: Byte): Byte {
            return if (v < 27) (27 + v).toByte() else v
        }

        private fun addStart(message: String): Binary {
            val resultMessage = START + message.length + message
            return Binary.apply(resultMessage.toByteArray(StandardCharsets.US_ASCII))
        }
    }
}
