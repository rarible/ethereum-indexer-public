package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PoolTargetNftOutTest {
    @Test
    fun `set marker - ok`() {
        val out = randomPoolTargetNftOut().copy(marketplaceMarker = null)
        val input = Binary.apply("00000000000000000000000000000000000000000000000109616c6c64617461")
        val updateOut = PoolTargetNftOut.addMarketplaceMarker(out, input)
        assertThat(updateOut.marketplaceMarker).isNotNull
    }
}
