package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
import io.daonomic.rpc.domain.Binary
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class ExchangeWrapperUnitTest {
    private val traceCallService: TraceCallService = mockk()

    private val openSeaOrderParser = OpenSeaOrderParser(
        traceCallService = traceCallService,
        callDataEncoder = CallDataEncoder(),
        featureFlags = OrderIndexerProperties.FeatureFlags()
    )

    @Test
    fun `convert - fee values in singlePurchase`() {
        val fees = Binary.apply("0x0000000000000000000000000000000000000000000000000000000103e807d0").toBigInteger()
        val address1 = randomAddress()
        val address2 = randomAddress()
        val parts = openSeaOrderParser.convertToFeePart(fees, address1, address2)
        assertThat(parts).hasSize(2)
        assertThat(parts[0].account).isEqualTo(address1)
        assertThat(parts[0].value.value).isEqualTo(BigInteger("1000"))
        assertThat(parts[1].account).isEqualTo(address2)
        assertThat(parts[1].value.value).isEqualTo(BigInteger("2000"))
    }
}
