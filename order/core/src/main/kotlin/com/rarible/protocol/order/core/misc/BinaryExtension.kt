package com.rarible.protocol.order.core.misc

import io.daonomic.rpc.domain.Binary
import java.math.BigInteger
import java.util.Arrays

operator fun Binary.plus(other: Binary): Binary {
    return this.add(other)
}

fun Binary.clearAfter(offset: Int): Binary {
    if (offset >= (length() - 1)) return this
    val bytes = Arrays.copyOfRange(bytes(), 0, length())

    for (i in offset until length()) {
        bytes[i] = 0
    }
    return Binary.apply(bytes)
}

fun Binary?.orEmpty(): Binary {
    return this ?: Binary.apply()
}

fun Binary.methodSignatureId(): Binary? = if (length() >= 4) slice(0, 4) else null

fun String.toBinary(): Binary = Binary.apply(this)

fun String.paddingHex(): String {
    return if (this == "0x") {
        this
    } else if (this.length % 2 != 0) {
        val wrongHex = this.removePrefix("0x")
        "0x0$wrongHex"
    } else {
        this
    }
}

fun String.fromHexToBigInteger(): BigInteger? {
    val binary = paddingHex().toBinary()
    return if (binary != Binary.empty()) binary.toBigInteger() else null
}
