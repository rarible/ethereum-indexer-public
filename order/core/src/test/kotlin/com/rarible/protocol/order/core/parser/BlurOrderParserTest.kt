package com.rarible.protocol.order.core.parser

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.model.BlurOrderSide
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

class BlurOrderParserTest {
    @Test
    fun `parse cancel order tx`() {
        val blurOrders = BlurOrderParser.parserOrder(cancelOrderTx, Word.apply(randomWord()))
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

    @Test
    fun `parse execute tx`() {
        val executions = BlurOrderParser.parseExecutions(matchOrderTxData, Word.apply(randomWord()))
        assertThat(executions).hasSize(1)

        val execution = executions.single()
        assertThat(execution.sell.isEmptySignature()).isFalse
        assertThat(execution.buy.isEmptySignature()).isTrue
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
    //https://etherscan.io/tx/0xc10e943c243ff056278991efbd5221d4042a7515f2ef06cab25dfce03acc1a4b
    private val matchOrderTxData = Binary.apply(
        "0x9a1fc3a7000000000000000000000000000000000000000000000000000000" +
                "0000000040000000000000000000000000000000000000000000000000000" +
                "00000000004800000000000000000000000000000000000000000000000000" +
                "0000000000000e000000000000000000000000000000000000000000000000" +
                "0000000000000001be7a8b1a4b95509caecf77d340a28e09b860b77b1a45f6" +
                "142dea47cad752c51ff18c4079a556928bae43f922406491c08132eb96c784" +
                "9b9303d8b19fae8511bc200000000000000000000000000000000000000000" +
                "00000000000000000000320000000000000000000000000000000000000000" +
                "00000000000000000000000010000000000000000000000000000000000000" +
                "000000000000000000000fbb5030000000000000000000000003d606dd3a1b" +
                "8e2519f9f955439e52184d4c80fa3000000000000000000000000000000000" +
                "00000000000000000000000000000010000000000000000000000000000000" +
                "000dab4a563819e8fd93dba3b25bc349500000000000000000000000060e4d" +
                "786628fea6478f785a6d7e704777c86a7c6000000000000000000000000000" +
                "00000000000000000000000000000000034660000000000000000000000000" +
                "00000000000000000000000000000000000000100000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000c1deb315b74080000000000000000000000" +
                "000000000000000000000000000000000000063d3576d00000000000000000" +
                "00000000000000000000000000000000000000063dc91ed000000000000000" +
                "00000000000000000000000000000000000000000000001a00000000000000" +
                "000000000000000000008d88f2b78d2be0e687ac7ac6d60edc100000000000" +
                "00000000000000000000000000000000000000000000000000200000000000" +
                "00000000000000000000000000000000000000000000000000000010000000" +
                "00000000000000000000000000000000000000000000000000000003200000" +
                "0000000000000000000a858ddc0445d8131dac4d1de01f834ffcba52ef1000" +
                "00000000000000000000000000000000000000000000000000000000000010" +
                "10000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "10000000000000000000000000000000000000000000000000000000000000" +
                "00080000000000000000000000000000000000000000000000000000000000" +
                "000001be61d08d0a351eec5cbd6121633d89c17b7c6a75594875ef1cd8f080" +
                "d0ce665c73b4becd5594f78791f12e14b9396b41719dbbb058dce9f1105fbc" +
                "1674d6b55a3000000000000000000000000000000000000000000000000000" +
                "0000000000003be71759cad8c3f62933f749b4c3303b5b3021722c15587a3c" +
                "66d0d710caf1128f9785f168b87be4db00de6ab36305c3fa4327a84d502264" +
                "8cd76497196ed96d5f64daede2a122378484fef80c6702f225a50a30f9b57a" +
                "7aa02a7e7120bd8b37c0000000000000000000000000000000000000000000" +
                "0000000000000000000e000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000002e0000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000fbb5030000000000000000000000004aee9" +
                "1e7d6c6e0c4c0ec10b9757b8d19ab7f427c000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000dab4a563819e8fd93dba3b25bc349500000000000000000000000" +
                "060e4d786628fea6478f785a6d7e704777c86a7c6000000000000000000000" +
                "00000000000000000000000000000000000000034660000000000000000000" +
                "00000000000000000000000000000000000000000000100000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000c1deb315b74080000000000000000" +
                "000000000000000000000000000000000000000000063d3576e00000000000" +
                "00000000000000000000000000000000000000000000063d365f8000000000" +
                "00000000000000000000000000000000000000000000000000001a00000000" +
                "0000000000000000000000000d87b7d4a408d7f682088aec27ae7dbad00000" +
                "000000000000000000000000000000000000000000000000000000001c0000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "10100000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00060000000000000000000000000000000000000000000000000000000000" +
                "000001c651610a7411c8d076c2d99bf1573ea4a07a860a9393fb74030001c2" +
                "78a8de5ef20e42f8a60a9de78be2a8592fef865a7483c38173c4c126b62673" +
                "82bea38dc10"
    )
}