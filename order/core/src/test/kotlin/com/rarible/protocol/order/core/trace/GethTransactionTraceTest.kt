package com.rarible.protocol.order.core.trace

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import scalether.core.MonoEthereum
import scalether.domain.Address

class GethTransactionTraceTest {

    @Test
    @Tag("manual")
    fun `find trace work for geth`() = runBlocking<Unit> {
        val ethereum = MonoEthereum(WebClientTransport("https://node-rinkeby.rarible.com", MonoEthereum.mapper(), 10000, 10000))
        val testing = GethTransactionTraceProvider(ethereum)
        val traceResult = testing.traceAndFindCallTo(
            Word.apply("0x44ed2d81065b98d33bbb6bf0c409422efe8bd746189fd2a1a04f7effa89b7a80"),
            Address.apply("0x04792a5109e55d518db3b65285516cca55db46fe"),
            Binary.apply("0xe6a43905")
        )
        assertThat(traceResult?.input)
            .isEqualTo(Binary.apply("0xe6a43905000000000000000000000000b83a6d7f5dc224e241989511ea3e2b7f4f263ede000000000000000000000000ec23daeab1deeb3587eeb3453d4e95db128b0e62"))
    }
}