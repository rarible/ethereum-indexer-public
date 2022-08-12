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

class OpenEthereumTransactionTraceTest {

    @Test
    @Tag("manual")
    @Disabled
    fun `find all traces for openethereum`() = runBlocking<Unit> {
        val ethereum = MonoEthereum(object : WebClientTransport("https://node-mainnet.rarible.com", MonoEthereum.mapper(), 60000, 60000) {
            override fun maxInMemorySize(): Int = 100000000
        })
        val testing = OpenEthereumTransactionTraceProvider(ethereum)
        val traceResult = testing.traceAndFindAllCallsTo(
            Word.apply("0x3163b526e47333c9e66affb3124544e963ef0126bf9c6a3abfdaf30dd47efd7f"),
            Address.apply("0x7f268357a8c2552623316e2562d90e642bb538e5"),
            setOf(Binary.apply("0xab834bab"))
        )
        assertThat(traceResult.count()).isEqualTo(18)
    }
}
