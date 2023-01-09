package com.rarible.protocol.order.core.service

import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommonSignerTest {
    private val commonSigner = CommonSigner()

    @Test
    fun fix() {
        val signature = Binary.apply("0xeb92281fe15e71f9f3637f8a590f3b2682466addf9e8df521b64b744fb20efff2864850a199d0b5d8cbc3411e599a15c576d658e573b60c14f50a5228c2940d901")
        val fixedSignature = commonSigner.fixSignature(signature)
        assertThat(fixedSignature).isEqualTo(Binary.apply("0xeb92281fe15e71f9f3637f8a590f3b2682466addf9e8df521b64b744fb20efff2864850a199d0b5d8cbc3411e599a15c576d658e573b60c14f50a5228c2940d91c"))
    }
}