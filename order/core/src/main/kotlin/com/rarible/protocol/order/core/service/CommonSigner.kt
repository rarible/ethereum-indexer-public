package com.rarible.protocol.order.core.service

import com.rarible.ethereum.common.keccak256
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import scalether.domain.Address
import java.nio.charset.StandardCharsets

@Component
class CommonSigner {
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
        val v = fixV(signature.bytes()[64])
        val r = ByteArray(32)
        val s = ByteArray(32)
        System.arraycopy(signature.bytes(), 0, r, 0, 32)
        System.arraycopy(signature.bytes(), 32, s, 0, 32)
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

        private fun fixV(v: Byte): Byte {
            return if (v < 27) (27 + v).toByte() else v
        }

        private fun addStart(message: String): Binary {
            val resultMessage = START + message.length + message
            return Binary.apply(resultMessage.toByteArray(StandardCharsets.US_ASCII))
        }
    }
}
