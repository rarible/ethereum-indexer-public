package com.rarible.protocol.order.core.model

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.contracts.blur.v1.evemts.OrdersMatchedEvent
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.parser.BlurOrderParser
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BlurOrderTest {
    @Test
    fun `calculate test`() {
        val order = BlurOrderParser.parseExecutions(txInput, Word.apply(randomWord())).single()
        val event = OrdersMatchedEvent.apply(log)

        assertThat(
            Order.blurV1Hash(order.sell.order, BigInteger.ZERO)
        ).isEqualTo(Word.apply(event.sellHash()))

        assertThat(
            Order.blurV1Hash(order.buy.order, BigInteger.ZERO)
        ).isEqualTo(Word.apply(event.buyHash()))
    }


    //https://etherscan.io/tx/0x8dcd41cc28a34d22b9e03bc668566c255c5b01327018c381f496a1055ed90685
    private val log = log(
        topics = listOf(
            Word.apply("0x61cbb2a3dee0b6064c2e681aadd61677fb4ef319f0b547508d495626f5a62f64"),
            Word.apply("0x000000000000000000000000f1e9f0dd1d4e7039a423f3fc751180fea0f179fc"),
            Word.apply("0x0000000000000000000000000a9931ab317d8398e7316f7889f6ca39de328699"),
        ),
        data = "0000000000000000000000000000000000000000000000000000000000000080" +
                "3deb4923361bfbc4a205b4859ca0fe48779f0fa336e6ae361da1cbba143ac0f3" +
                "00000000000000000000000000000000000000000000000000000000000002c0" +
                "4ebbab2c24a1c31b351e9907c25656819432310dcfab1dc2b9397a3861ddb8d0" +
                "000000000000000000000000f1e9f0dd1d4e7039a423f3fc751180fea0f179fc" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000dab4a563819e8fd93dba3b25bc3495" +
                "000000000000000000000000d774557b647330c91bf44cfeab205095f7e6c367" +
                "000000000000000000000000000000000000000000000000000000000000455d" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000006ccd46763f10000" +
                "000000000000000000000000000000000000000000000000000000006423c38a" +
                "000000000000000000000000000000000000000000000000000000006425150a" +
                "00000000000000000000000000000000000000000000000000000000000001a0" +
                "00000000000000000000000000000000c9380b2b9c66391a77465720705c3129" +
                "0000000000000000000000000000000000000000000000000000000000000200" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "00000000000000000000000000000000000000000000000000000000000001f4" +
                "000000000000000000000000fca634387cd89128116b80e04c6352a4e7c5a40c" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0100000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000a9931ab317d8398e7316f7889f6ca39de328699" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000dab4a563819e8fd93dba3b25bc3495" +
                "000000000000000000000000d774557b647330c91bf44cfeab205095f7e6c367" +
                "000000000000000000000000000000000000000000000000000000000000455d" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000006ccd46763f10000" +
                "000000000000000000000000000000000000000000000000000000006423c38b" +
                "000000000000000000000000000000000000000000000000000000006423d2ce" +
                "00000000000000000000000000000000000000000000000000000000000001a0" +
                "00000000000000000000000000000000b0dcaca9ca69344c4ef51b570e5a9d51" +
                "00000000000000000000000000000000000000000000000000000000000001c0" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0100000000000000000000000000000000000000000000000000000000000000"

    )

    private val txInput = Binary.apply(
        "0x9a1fc3a7000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000" +
                "0000000000000000000000000000046000000000000000000000000000000000000000000000000000000000000000e00000000" +
                "00000000000000000000000000000000000000000000000000000001bf99b88d2cbdff6344c46a88a5b3f01aa1db3e6c603e458" +
                "813d7dacf149d319547856ea2d9241fe3d2c6f7626777605501b88423d000b0d5ba02c6daa9c42c9cd000000000000000000000" +
                "0000000000000000000000000000000000000000320000000000000000000000000000000000000000000000000000000000000" +
                "00010000000000000000000000000000000000000000000000000000000001025796000000000000000000000000f1e9f0dd1d4" +
                "e7039a423f3fc751180fea0f179fc00000000000000000000000000000000000000000000000000000000000000010000000000" +
                "000000000000000000000000dab4a563819e8fd93dba3b25bc3495000000000000000000000000d774557b647330c91bf44cfea" +
                "b205095f7e6c367000000000000000000000000000000000000000000000000000000000000455d000000000000000000000000" +
                "0000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000006ccd46763f1000000000000000000000000000000000000000000" +
                "0000000000000000006423c38a000000000000000000000000000000000000000000000000000000006425150a0000000000000" +
                "0000000000000000000000000000000000000000000000001a000000000000000000000000000000000c9380b2b9c66391a7746" +
                "5720705c31290000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000" +
                "000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000001f400" +
                "0000000000000000000000fca634387cd89128116b80e04c6352a4e7c5a40c00000000000000000000000000000000000000000" +
                "0000000000000000000000101000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000" +
                "000000080000000000000000000000000000000000000000000000000000000000000001bf2c05cf8533852d440579b5f736313" +
                "6b3c60300e6805365fe145194195aab65b2c909456b89c7992f17ad477434e048babbe4994ff881e90a4967d13f79e7c4200000" +
                "000000000000000000000000000000000000000000000000000000000021eed67759cc6eef6df039beb08830865099171581725" +
                "cead5820590be97dd262010bb675e62f8474a492f556e3fda02ae78e743b4a6fe348bd2ad9a00386bb870000000000000000000" +
                "0000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e0000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000010257960000000000000000000000000a9931ab317d8398e7316f7889f6ca39de32869900000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000dab4a563819e8fd93dba3b25bc34" +
                "95000000000000000000000000d774557b647330c91bf44cfeab205095f7e6c3670000000000000000000000000000000000000" +
                "00000000000000000000000455d0000000000000000000000000000000000000000000000000000000000000001000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006cc" +
                "d46763f10000000000000000000000000000000000000000000000000000000000006423c38b0000000000000000000000000000" +
                "00000000000000000000000000006423d2ce00000000000000000000000000000000000000000000000000000000000001a00000" +
                "0000000000000000000000000000b0dcaca9ca69344c4ef51b570e5a9d5100000000000000000000000000000000000000000000" +
                "000000000000000001c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000001010000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000" +
                "000000000000000000000000001c8114878bdfb586d2977fefa52adad8be472e73c571d26037f37f9f13671aa53c433e4a039d94" +
                "5288ae8e5b6381d21a061c831ff8c8e1a58de3c3c1fe1cf4f7c5"
    )
}