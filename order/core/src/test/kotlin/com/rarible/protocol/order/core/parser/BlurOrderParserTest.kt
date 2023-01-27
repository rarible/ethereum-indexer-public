package com.rarible.protocol.order.core.parser

import com.rarible.protocol.order.core.model.BlurOrderSide
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class BlurOrderParserTest {
    @Test
    fun `parse cancel order tx`() {
        val blurOrders = BlurOrderParser.parserOrder(cancelOrderTx)
        assertThat(blurOrders).hasSize(1)

        val order = blurOrders.single()
        assertThat(order.trader).isEqualTo(Address.apply("0x1ab608B76dE67507E1d70441Ee9282FEFa17a334"))
        assertThat(order.side).isEqualTo(BlurOrderSide.SELL)
        assertThat(order.matchingPolicy).isEqualTo(Address.apply("0x0000000000daB4A563819e8fd93dbA3b25BC3495"))
        assertThat(order.collection).isEqualTo(Address.apply("0x394E3d3044fC89fCDd966D3cb35Ac0B32B0Cda91"))
        assertThat(order.tokenId).isEqualTo(BigInteger("7774"))
        assertThat(order.amount).isEqualTo(BigInteger.ONE)
        assertThat(order.paymentToken).isEqualTo(Address.ZERO())
        assertThat(order.price).isEqualTo(BigInteger("1370000000000000000"))
        assertThat(order.listingTime).isEqualTo(BigInteger("1674662172"))
        assertThat(order.expirationTime).isEqualTo(BigInteger("1675266971"))
        assertThat(order.fees.single().rate).isEqualTo(BigInteger("50"))
        assertThat(order.fees.single().recipient).isEqualTo(Address.apply("0x60D190772500FaCa6a32e2b88fF0cFE5D9D75142"))
        assertThat(order.salt).isEqualTo(BigInteger("85307418654722842231528938399381756281"))
        assertThat(order.extraParams).isEqualTo(Binary.apply("0x01"))
    }

    //https://etherscan.io/tx/0x3e9721a6c90445d3847a6792138f856cc14891f027af233f5b52c4c3463ceb22
    private val cancelOrderTx = Binary.apply(
        "0xf4acd74000000000000000000000000000000000000000000000000000000000000000200000000000000000000000001ab608b7" +
                "6de67507e1d70441ee9282fefa17a33400000000000000000000000000000000000000000000000000000000000000010000000" +
                "000000000000000000000000000dab4a563819e8fd93dba3b25bc3495000000000000000000000000394e3d3044fc89fcdd966d" +
                "3cb35ac0b32b0cda910000000000000000000000000000000000000000000000000000000000001e5e000000000000000000000" +
                "0000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000130337bdce49000000000000000000000000000000000000000" +
                "00000000000000000000063d1511c0000000000000000000000000000000000000000000000000000000063da8b9b0000000000" +
                "0000000000000000000000000000000000000000000000000001a000000000000000000000000000000000402d9c7808533880c" +
                "d3f9b2982b299790000000000000000000000000000000000000000000000000000000000000200000000000000000000000000" +
                "0000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000003" +
                "200000000000000000000000060d190772500faca6a32e2b88ff0cfe5d9d7514200000000000000000000000000000000000000" +
                "000000000000000000000000010100000000000000000000000000000000000000000000000000000000000000"
    )
}