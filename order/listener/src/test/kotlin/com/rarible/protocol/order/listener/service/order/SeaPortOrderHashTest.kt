package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.order.core.service.CommonSigner
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class SeaPortOrderHashTest {
    private val commonSigner = CommonSigner()
    @Test
    fun `should verify SeaPortOrder signature `() {
        val domain = EIP712Domain(
            name = "Seaport",
            version = "1.1",
            chainId = BigInteger.ONE,
            verifyingContract = Address.apply("0x00000000006c3852cbef3e08e8df289169ede581")
        )
        val maker = Address.apply("0xaf681629f3c577afa464daa566d9d1d9a7cdafe3")
        val signature = Binary.apply("0x0ec3d527940f303961a66ce7882cd48daa51bdfd2b76cf1a86f9a0cee2033217207088b8791645f44678a64b1deb9722a91340288d94a179989777bc3f22036b1c")

        val orderHash = Word.apply("0xe8aaf8955174b66272217d6cdee1235a875d4279d5e826636c568f984fba64ba")
        val hashToSign = domain.hashToSign(orderHash)

        val result = commonSigner.recover(hashToSign, signature)
        assertThat(result).isEqualTo(maker)
    }
}
