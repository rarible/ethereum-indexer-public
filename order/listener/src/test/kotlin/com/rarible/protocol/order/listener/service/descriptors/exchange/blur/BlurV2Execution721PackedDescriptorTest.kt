package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal

class BlurV2Execution721PackedDescriptorTest : AbstractBlurV2ExecutionDescriptorTest() {
    private val descriptor = BlurV2Execution721PackedDescriptor(
        contractsProvider = contractsProvider,
        blurV2EventConverter = blurV2EventConverter,
    )

    @Test
    fun `convert simple hash`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0xc199fd412393246039e22dcd70a8231bad64acee868b4e08349f9ec6509284d5
        val log = log(
            topics = listOf(
                Word.apply("0x1d5e12b51dee5e4d34434576c3fb99714a85f57b0fd546ada4b0bddd736d12b2"),
            ),
            data = "0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa4000000000000000000020060083fa03a1747aafe4e0f0ec3880740f49d74f11580000000000354a6ba7a18000789e35a999c443fe6089544056f728239b8ffee7"
        )
        val transient = mockkTransaction(execution721PackedTx)
        val ethereumBlockchainLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transient,
            index = 0,
            total = 1
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x789e35a999c443fe6089544056f728239b8ffee7"),
                EthUInt256.of(8198)
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("015000000000000000")
        )

        val matches = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog)
        Assertions.assertThat(matches).hasSize(2)

        val left = matches.single { it.side == OrderSide.LEFT }
        Assertions.assertThat(left.hash).isEqualTo(Word.apply("0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa40"))
        Assertions.assertThat(left.maker).isEqualTo(Address.apply("0x83fa03a1747aafe4e0f0ec3880740f49d74f1158"))
        Assertions.assertThat(left.taker).isEqualTo(Address.apply("0xf770e42fd25d745e9f25ab57cca3e96f7fe62d14"))
        Assertions.assertThat(left.make).isEqualTo(expectedNft)
        Assertions.assertThat(left.take).isEqualTo(expectedPayment)
        Assertions.assertThat(left.make).isEqualTo(expectedNft)
        Assertions.assertThat(left.take).isEqualTo(expectedPayment)
        Assertions.assertThat(left.fill).isEqualTo(expectedNft.value)
        Assertions.assertThat(left.date).isEqualTo(blockTimestamp)
        Assertions.assertThat(left.takeValue).isEqualTo(BigDecimal("0.015000000000000000"))
        Assertions.assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(left.source).isEqualTo(HistorySource.BLUR)
        Assertions.assertThat(left.adhoc).isFalse
        Assertions.assertThat(left.counterAdhoc).isTrue
        Assertions.assertThat(left.originFees).isEmpty()

        val right = matches.single { it.side == OrderSide.RIGHT }
        Assertions.assertThat(right.counterHash).isEqualTo(Word.apply("0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa40"))
        Assertions.assertThat(right.maker).isEqualTo(Address.apply("0xf770e42fd25d745e9f25ab57cca3e96f7fe62d14"))
        Assertions.assertThat(right.taker).isEqualTo(Address.apply("0x83fa03a1747aafe4e0f0ec3880740f49d74f1158"))
        Assertions.assertThat(right.make).isEqualTo(expectedPayment)
        Assertions.assertThat(right.take).isEqualTo(expectedNft)
        Assertions.assertThat(right.fill).isEqualTo(expectedPayment.value)
        Assertions.assertThat(right.date).isEqualTo(blockTimestamp)
        Assertions.assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        Assertions.assertThat(right.makeValue).isEqualTo(BigDecimal("0.015000000000000000"))
        Assertions.assertThat(right.source).isEqualTo(HistorySource.BLUR)
        Assertions.assertThat(right.adhoc).isTrue
        Assertions.assertThat(right.counterAdhoc).isFalse
        Assertions.assertThat(right.originFees).isEmpty()
    }

    private val execution721PackedTx = Binary.apply(
        "0x70bce2d6000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000003a000000000000000000000000083fa03a1747aafe4e0f0ec3880740f49d74f1158000000000000000000000000789e35a999c443fe6089544056f728239b8ffee7fe5511c35e70a0393b16a2240106f5e64b2b05580511231fda3ca216b9690cb100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000064c0c12600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ea91ac90a6b98aec6a1cc324faff58b000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e0000000000000000000000000f770e42fd25d745e9f25ab57cca3e96f7fe62d140000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002006000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000354a6ba7a180000000000000000000000000000000000000000000000000000000000000002006000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000041c71899e4d8cbcc6d55b03ee381c781b8ad8f0edc0154eec793c396b0d13fd9ea122bcf8407fd688f2a89ce493cef4eb50ee87750557701f95e5fe349d959fde91b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000059662800ebc45c9ea81489b7885ff02c7dae30ed549d2c8ccf9405de945126e6473a1e8ee8795be93065c797cd87eb563aa27f74a98eda5243952f0bdc08b291821b010e786f6af68e5d010513ff70a3aaed9afeb8661116e6ce00000000000000332d1229"
    )

}

