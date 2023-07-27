package com.rarible.protocol.order.listener.misc

import com.rarible.protocol.order.core.misc.toBinary
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.web3jold.crypto.Sign
import java.math.BigInteger

fun Word.sign(privateKey: BigInteger): Binary {
    val publicKey = Sign.publicKeyFromPrivate(privateKey)
    return Sign.signMessageHash(bytes(), publicKey, privateKey).toBinary()
}
