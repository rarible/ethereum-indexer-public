package com.rarible.protocol.order.core.trace

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import scalether.core.MonoEthereum
import scalether.domain.Address

class GethTransactionTraceTest {

    @Test
    @Tag("manual")
    @Disabled
    fun `find trace work for geth`() = runBlocking<Unit> {
        val ethereum = MonoEthereum(WebClientTransport("https://node-rinkeby.rarible.com", MonoEthereum.mapper(), 10000, 10000))
        val testing = GethTransactionTraceProvider(ethereum)
        val traceResults = testing.traceAndFindAllCallsTo(
            Word.apply("0x417a49506dadc14bae73c201b8f36aeb8186c98aa9b31c0a423abbd420f3d618"),
            Address.apply("0xd4a57a3bd3657d0d46b4c5bac12b3f156b9b886b"),
            setOf(Binary.apply("0xe6a43905"), Binary.apply("0x0d5f7d35"))
        )
        traceResults.forEach {
            println(it)
        }
        assertThat(traceResults.size).isEqualTo(2)
        val traceResult = traceResults.firstOrNull()
        assertThat(traceResult?.input)
            .isEqualTo(Binary.apply("0x0d5f7d35000000000000000000000000000000000000000000000000000000000000002000000000000000000000000022d491bde2303f2f43325b2108d26f1eaba1e32b000000000000000000000000000000000000000000000000000000000000000173ad21460000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e000000000000000000000000000000000000000000000000000000000000f424000000000000000000000000000000000000000000000000000000000000000005b722d90780b8030a4f1369e606f303d0a515dea0152f74fc0e6b3cab5d0d9260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000023d235ef000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000240000000000000000000000000000000000000000000000000000000000000032000000000000000000000000000000000000000000000000000000000000f4240000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000003a000000000000000000000000000000000000000000000000000000000000000400000000000000000000000006ede7f3c26975aad32a475e1021d8f6f39c89d8222d491bde2303f2f43325b2108d26f1eaba1e32b00000000000000000000019100000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000412898dfd7506943aa53d30b3adee498c432aee590360b3bdafec367e5eddbb147133644660adf689a75f92a18924d5fbfed394ebd5eff4d42fafde898ccb554561b0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001400000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c66d094ed928f7840a6b0d373c1cd825c97e3c7c000000000000000000000000000000000000000000000000000000000000271000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000d28e9bd340e48370475553d21bd0a95c9a60f920000000000000000000000000000000000000000000000000000000000000064"))
    }
}
