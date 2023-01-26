package com.rarible.protocol.order.listener.service.blur

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

class BlurEventConverterTest {
    private val traceCallService = mockk<TraceCallService>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val standardProvider = mockk<TokenStandardProvider>()

    private val converter = BlurEventConverter(traceCallService, featureFlags, standardProvider)

    @Test
    fun `convert cancel`() = runBlocking<Unit> {
        val collection = Address.apply("0x394E3d3044fC89fCDd966D3cb35Ac0B32B0Cda91")
        val tokenId = EthUInt256.of(7774)
        val log = log(
            topics = listOf(
                Word.apply("0x5152abf959f6564662358c2e52b702259b78bac5ee7842a0f01937e670efcc7d"),
            ),
            data = "0xB9D227C83C244B1F4D05F322ACD8CF78062F1AD8FF4DE58A2639D3BBB2308572"
        )
        val transient = mockk<Transaction> {
            every { input() } returns cancelOrderTx
        }
        coEvery {
            standardProvider.getTokenStandard(collection)
        } returns TokenStandard.ERC721

        val expectedDate = Instant.now()
        val expectedMake = Asset(Erc721AssetType(collection, tokenId), EthUInt256.ONE)
        val expectedTake = Asset(EthAssetType, EthUInt256.of(BigInteger("1370000000000000000")))

        val cancels = converter.convert(log, transient, 0, 1, expectedDate)
        assertThat(cancels).hasSize(1)
        assertThat(cancels.single().hash).isEqualTo(Word.apply("0xB9D227C83C244B1F4D05F322ACD8CF78062F1AD8FF4DE58A2639D3BBB2308572"))
        assertThat(cancels.single().source).isEqualTo(HistorySource.BLUR)
        assertThat(cancels.single().date).isEqualTo(expectedDate)
        assertThat(cancels.single().make).isEqualTo(expectedMake)
        assertThat(cancels.single().take).isEqualTo(expectedTake)
        assertThat(cancels.single().maker).isEqualTo(Address.apply("0x1ab608B76dE67507E1d70441Ee9282FEFa17a334"))
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