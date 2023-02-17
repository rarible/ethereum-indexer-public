package com.rarible.protocol.order.core.misc

import io.daonomic.rpc.domain.Binary
import org.web3j.crypto.Sign
import scala.Tuple3

fun Sign.SignatureData.toBinary(): Binary =
    Binary.apply(this.r).add(this.s).add(byteArrayOf(this.v))

fun Binary.toSignatureData() =
    Sign.SignatureData(this.bytes()[64], this.slice(0, 32).bytes(), this.slice(32, 64).bytes())

fun Sign.SignatureData.fixV(): Sign.SignatureData {
    return if (v < 27) {
        Sign.SignatureData((27 + v).toByte(), r, s)
    } else {
        this
    }
}

fun buildSignature(v: Byte, r: Binary, s: Binary): Binary {
    return Binary.apply(byteArrayOf(v)).add(s).add(r)
}

fun Sign.SignatureData.toTuple() =
    Tuple3(v.toInt().toBigInteger(), r, s)

val EMPTY_SIGNATURE = Sign.SignatureData(0, ByteArray(32), ByteArray(32))
