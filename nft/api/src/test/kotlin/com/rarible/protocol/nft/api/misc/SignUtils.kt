package com.rarible.protocol.nft.api.misc

import com.rarible.protocol.nft.core.model.SignedLong
import io.daonomic.rpc.domain.Binary
import org.web3j.crypto.Sign
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger

class SignUtils {
    companion object {
        fun sign(privateKey: BigInteger, value: Long, address: Address? = null): SignedLong {
            val publicKey = Sign.publicKeyFromPrivate(privateKey)
            val toSign = if (address != null) {
                address.add(Uint256Type.encode(BigInteger.valueOf(value)))
            } else {
                Uint256Type.encode(BigInteger.valueOf(value))
            }
            val signed = Sign.signMessage(toSign.bytes(), publicKey, privateKey)
            return SignedLong(value, signed.v, Binary(signed.r), Binary(signed.s))
        }
    }
}