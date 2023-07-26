package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.misc.EMPTY_SIGNATURE
import io.daonomic.rpc.domain.Binary
import org.web3jold.crypto.Sign
import java.math.BigInteger

data class BlurInput(
    val order: BlurOrder,
    val v: BigInteger,
    val r: Binary,
    val s: Binary
) {
    fun isEmptySignature(): Boolean {
        return isEmptyData() || signatureData() == EMPTY_SIGNATURE
    }

    private fun isEmptyData(): Boolean {
        return v == BigInteger.ZERO && r.length() == 0 && s.length() == 0
    }

    private fun signatureData(): Sign.SignatureData {
        return Sign.SignatureData(v.byteValueExact(), r.bytes(), s.bytes())
    }
}
