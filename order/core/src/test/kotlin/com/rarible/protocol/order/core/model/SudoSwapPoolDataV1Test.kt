package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.createSudoSwapPoolDataV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SudoSwapPoolDataV1Test {
    @Test
    fun convertTest() {
        val poolData = createSudoSwapPoolDataV1()
        val orderData = poolData.toOrderData()
        assertThat(orderData.poolAddress).isEqualTo(poolData.poolAddress)
        assertThat(orderData.bondingCurve).isEqualTo(poolData.bondingCurve)
        assertThat(orderData.curveType).isEqualTo(poolData.curveType)
        assertThat(orderData.assetRecipient).isEqualTo(poolData.assetRecipient)
        assertThat(orderData.poolType).isEqualTo(poolData.poolType)
        assertThat(orderData.delta).isEqualTo(poolData.delta)
        assertThat(orderData.fee).isEqualTo(poolData.fee)
    }
}
