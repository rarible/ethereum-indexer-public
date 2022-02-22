package com.rarible.protocol.order.core.service

import com.rarible.ethereum.sign.domain.EIP712Domain
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class OpenSeaSignerTest {
    @Test
    fun `eip712 hashToSign should work`() {
        val s = OpenSeaSigner(CommonSigner(), EIP712Domain("Wyvern Exchange Contract", "2.3", BigInteger.valueOf(4), Address.apply("0xdd54d660178b28f6033a953b0e55073cfa7e3744")))
        assertThat(s.openSeaHashToSign(Word.apply("0x1879973b20431b35d0bc0ba145b95fd2f2f3b5676a232fd2c78aa2f87b3703d8"), true))
            .isEqualTo(Word.apply("0x0c2c7210b3fa1f185993d406b9bcdf551d66519f082646498873e3ffdb58f253"))
    }
}