package com.rarible.protocol.order.core.trace

import com.rarible.protocol.contracts.exchange.v2.rev3.ExchangeV2
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import scalether.domain.AddressFactory

class TraceCallServiceTest {

    @Test
    fun testMetaTxTraceCall() {
        val service = TraceCallServiceImpl(NoopTransactionTraceProvider(), OrderIndexerProperties.FeatureFlags())
        val transactionInput =
            ("0x0c53c51c00000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb000000000000000000" +
                "00000000000000000000000000000000000000000000a0ea63d80a19f03e50bbf98943043a744c6e98780958dfecf7f" +
                "2c2dd52354b1f337c32b1e457bf73846a463e0bd76698ba43df4ceab74820cf28066fb852b45c320000000000000000" +
                "00000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000" +
                "00000000000000864e99a3f800000000000000000000000000000000000000000000000000000000000000080000000" +
                "00000000000000000000000000000000000000000000000000000004200000000000000000000000000000000000000" +
                "0000000000000000000000004a000000000000000000000000000000000000000000000000000000000000008400000" +
                "0000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000000000000000" +
                "00000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000002007b5eef2a21a817facdfa3b911dce2967e" +
                "2fc25d6a5d6072f22d48def970073660000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000023d235ef00000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002" +
                "c0000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000" +
                "00000000000000000000000000000000002973bb6400000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000" +
                "000000000000000000000000000000000004000000000000000000000000067a8fe17db4d441f96f26094677763a221" +
                "3a3b5f19d2a55f2bd362a9e09f674b722782329f63f3fb0000000000000000000000050000000000000000000000000" +
                "00000000000000000000000000000000000004000000000000000000000000000000000000000000000000000071afd" +
                "498d00008ae85d840000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000" +
                "00000000200000000000000000000000009c3c9283d3e44854697cd22d3faa240cfb032889000000000000000000000" +
                "00000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000" +
                "00000000002000000000000000000000000000000000000000000000000000000000000000600000000000000000000" +
                "00000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000" +
                "00000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000004144c35af9080149422c54539a65e663ff8ae6e5ba8d61cac3ee35795c009618516dc77e3d0922c23" +
                "8c65b8708d79188350f9e880b7263e085179d93ea8efa297d1b00000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb0000000000000" +
                "00000000000000000000000000000000000000000000000012000000000000000000000000019d2a55f2bd362a9e09f" +
                "674b722782329f63f3fb00000000000000000000000000000000000000000000000000000000000001e000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000023d235ef0" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000002c000000000000000000000000000000000000000000000000000000000000000400000000" +
                "0000000000000000000000000000000000000000000038d7ea4c680008ae85d84000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000" +
                "000000000000000000000000000000000000000000000000000000000200000000000000000000000009c3c9283d3e4" +
                "4854697cd22d3faa240cfb0328890000000000000000000000000000000000000000000000000000000000000040000" +
                "0000000000000000000000000000000000000000000000000000000000001973bb64000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000400" +
                "00000000000000000000000000000000000000000000000000000000000004000000000000000000000000067a8fe17" +
                "db4d441f96f26094677763a2213a3b5f19d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000" +
                "500000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000" +
                "00000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000" +
                "06000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")

        val testResult = Binary.apply("0xe99a3f800000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000042000000000000000000000000000000000000000000000000000000000000004a0000000000000000000000000000000000000000000000000000000000000084000000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb0000000000000000000000000000000000000000000000000000000000000120000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002007b5eef2a21a817facdfa3b911dce2967e2fc25d6a5d6072f22d48def970073660000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000023d235ef0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000002973bb640000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000004000000000000000000000000067a8fe17db4d441f96f26094677763a2213a3b5f19d2a55f2bd362a9e09f674b722782329f63f3fb000000000000000000000005000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000071afd498d00008ae85d8400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000200000000000000000000000009c3c9283d3e44854697cd22d3faa240cfb03288900000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004144c35af9080149422c54539a65e663ff8ae6e5ba8d61cac3ee35795c009618516dc77e3d0922c238c65b8708d79188350f9e880b7263e085179d93ea8efa297d1b0000000000000000000000000000000000000000000000000000000000000000000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb000000000000000000000000000000000000000000000000000000000000012000000000000000000000000019d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000000000000000000000000000000000000000001e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000023d235ef0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002c0000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000038d7ea4c680008ae85d8400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000200000000000000000000000009c3c9283d3e44854697cd22d3faa240cfb03288900000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000001973bb640000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000004000000000000000000000000067a8fe17db4d441f96f26094677763a2213a3b5f19d2a55f2bd362a9e09f674b722782329f63f3fb00000000000000000000000500000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        runBlocking {
            val result = service.findAllRequiredCallInputs(WordFactory.create(), Binary.apply(transactionInput), AddressFactory.create(), ExchangeV2.matchOrdersSignature().id())
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(testResult)
        }
    }
}
