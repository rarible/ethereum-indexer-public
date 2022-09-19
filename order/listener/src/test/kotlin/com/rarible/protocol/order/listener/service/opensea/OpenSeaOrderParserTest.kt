package com.rarible.protocol.order.listener.service.opensea

import com.mongodb.internal.connection.tlschannel.util.Util.assertTrue
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.NoopTransactionTraceProvider
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.core.trace.TraceCallServiceImpl
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.WordFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

internal class OpenSeaOrderParserTest {
    private val parser = OpenSeaOrderParser(
        traceCallService = TraceCallServiceImpl(NoopTransactionTraceProvider(), OrderIndexerProperties.FeatureFlags()),
        callDataEncoder = CallDataEncoder(),
        featureFlags = OrderIndexerProperties.FeatureFlags()
    )

    private val callDataEncoder = CallDataEncoder()

    private val priceUpdateService = mockk<PriceUpdateService> {
        coEvery { getAssetsUsdValue(any(), any(), any()) } returns null
    }

    private val prizeNormalizer = mockk<PriceNormalizer> {
        coEvery { normalize(any()) } returns BigDecimal(0)
    }

    private val openSeaOrdersSideMatcher = OpenSeaOrderEventConverter(priceUpdateService, prizeNormalizer, callDataEncoder)

    @Test
    fun `should parse sell order simple test`() = runBlocking<Unit> {
        val input = "0xab834bab0000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000000000000000000000000000000000000000000000000000000000000000000004a6a5703a9796630e9fa04f5ecaf730065a7b827000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d90000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c010730000000000000000000000004a6a5703a9796630e9fa04f5ecaf730065a7b8270000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004e200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003311fc80a5700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102e03f00000000000000000000000000000000000000000000000000000000000000003f9b23e00ccc19a1e6399b0bf5df8a7cac4df96c79b1fdf548e85f00c13be3ea00000000000000000000000000000000000000000000000000000000000004e200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003311fc80a5700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102dff100000000000000000000000000000000000000000000000000000000000000006db950761e734348580ff301dadd346aadd7960399a7390f39cf0e6f66159b590000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006a0000000000000000000000000000000000000000000000000000000000000074000000000000000000000000000000000000000000000000000000000000007e0000000000000000000000000000000000000000000000000000000000000088000000000000000000000000000000000000000000000000000000000000009200000000000000000000000000000000000000000000000000000000000000940000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b8868021b1bb1de3b41ae8325a9934dfa004eabea54b4c1b76e353406abf1bea2644d1917aaed64d531c5ca7b47756cd91f461de089592d18fdb7fc176baf5c838868021b1bb1de3b41ae8325a9934dfa004eabea54b4c1b76e353406abf1bea2644d1917aaed64d531c5ca7b47756cd91f461de089592d18fdb7fc176baf5c830000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006400000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val price = BigInteger.valueOf(1000)
        val orders = parser.parseMatchedOrders(input)
        assertThat(orders).isNotNull

        val from = orders!!.buyOrder.maker
        val date = nowMillis()
        val matchers = openSeaOrdersSideMatcher.convert(orders, from, price, date, WordFactory.create())

        val buyMatch = matchers[0]
        val sellMatch = matchers[1]

        assertThat(sellMatch.hash).isEqualTo(orders.sellOrder.hash)
        assertThat(sellMatch.fill).isEqualTo(EthUInt256.of(price))
        assertThat(sellMatch.side).isEqualTo(OrderSide.LEFT)
        assertThat(sellMatch.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(sellMatch.taker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(sellMatch.make.type).isInstanceOf(NftAssetType::class.java)
        assertThat((sellMatch.make.type as NftAssetType).token).isEqualTo(Address.apply("0x4a6a5703a9796630e9fa04f5ecaf730065a7b827"))
        assertThat((sellMatch.make.type as NftAssetType).tokenId).isEqualTo(EthUInt256.of(10))
        assertThat(sellMatch.make.value).isEqualTo(EthUInt256.ONE)
        assertThat(sellMatch.take.type).isInstanceOf(EthAssetType::class.java)
        assertThat(sellMatch.take.value).isEqualTo(EthUInt256.of(price))
        assertThat(sellMatch.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(sellMatch.date).isEqualTo(date)
        assertThat(sellMatch.adhoc).isFalse()

        assertThat(buyMatch.hash).isEqualTo(orders.buyOrder.hash)
        assertThat(buyMatch.fill).isEqualTo(EthUInt256.ONE)
        assertThat(buyMatch.side).isEqualTo(OrderSide.RIGHT)
        assertThat(buyMatch.maker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(buyMatch.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(buyMatch.take.type).isInstanceOf(NftAssetType::class.java)
        assertThat((buyMatch.take.type as NftAssetType).token).isEqualTo(Address.apply("0x4a6a5703a9796630e9fa04f5ecaf730065a7b827"))
        assertThat((buyMatch.take.type as NftAssetType).tokenId).isEqualTo(EthUInt256.of(10))
        assertThat(buyMatch.take.value).isEqualTo(EthUInt256.ONE)
        assertThat(buyMatch.make.type).isInstanceOf(EthAssetType::class.java)
        assertThat(buyMatch.make.value).isEqualTo(EthUInt256.of(price))
        assertThat(buyMatch.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(buyMatch.date).isEqualTo(date)
        assertThat(buyMatch.adhoc).isTrue()

        coVerify(exactly = 2) { prizeNormalizer.normalize(withArg {
            assertTrue(it.type is EthAssetType)
        }) }
        coVerify(exactly = 2) { prizeNormalizer.normalize(withArg {
            assertTrue(it.type is Erc721AssetType)
        }) }
    }

    @Test
    fun `should parse bid order`() = runBlocking<Unit> {
        val input = "0xab834bab0000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c01073000000000000000000000000509fd4cdaa29be7b1fad251d8ea0fca2ca91eb600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c778417e063141139fce010982780140aa0cd5ab0000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d90000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000509fd4cdaa29be7b1fad251d8ea0fca2ca91eb600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c778417e063141139fce010982780140aa0cd5ab000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016345785d8a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102e57700000000000000000000000000000000000000000000000000000000610c204a409f1b813ec7af56e8e5e18db7be476fc81bb997af9d82f3f429d527f61337b6000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016345785d8a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102e5aa0000000000000000000000000000000000000000000000000000000000000000d8bd751c904c4988d75cacd57c39e117299e2d9d7ab171e866fd79f9d72267b10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006a0000000000000000000000000000000000000000000000000000000000000074000000000000000000000000000000000000000000000000000000000000007e0000000000000000000000000000000000000000000000000000000000000088000000000000000000000000000000000000000000000000000000000000009200000000000000000000000000000000000000000000000000000000000000940000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000001ceb069b4b46dc2f9bce002d54edf9653e7f556fa6ecdb70c7f44bb3295c0864e33210ec04d10e5b5dc76c338e37f583aa2587eeec4d2840eb073da2d93fc3ed4beb069b4b46dc2f9bce002d54edf9653e7f556fa6ecdb70c7f44bb3295c0864e33210ec04d10e5b5dc76c338e37f583aa2587eeec4d2840eb073da2d93fc3ed4b0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b07700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b07700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006400000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val orders = parser.parseMatchedOrders(input)
        assertThat(orders).isNotNull

        val from = orders!!.sellOrder.maker
        val date = nowMillis()
        val matchers = openSeaOrdersSideMatcher.convert(orders, from, BigInteger.valueOf(1000), date, WordFactory.create())
        val price = BigInteger.valueOf(1000)

        val buyMatch = matchers[0]
        val sellMatch = matchers[1]

        assertThat(buyMatch.hash).isEqualTo(orders.buyOrder.hash)
        assertThat(buyMatch.fill).isEqualTo(EthUInt256.ONE)
        assertThat(buyMatch.side).isEqualTo(OrderSide.LEFT)
        assertThat(buyMatch.maker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(buyMatch.taker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(buyMatch.make.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((buyMatch.make.type as Erc20AssetType).token).isEqualTo(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab"))
        assertThat(buyMatch.make.value).isEqualTo(EthUInt256.of(price))
        assertThat(buyMatch.take.type).isInstanceOf(NftAssetType::class.java)
        assertThat((buyMatch.take.type as NftAssetType).token).isEqualTo(Address.apply("0x509fd4cdaa29be7b1fad251d8ea0fca2ca91eb60"))
        assertThat((buyMatch.take.type as NftAssetType).tokenId).isEqualTo(EthUInt256.of(110711))
        assertThat(buyMatch.take.value).isEqualTo(EthUInt256.ONE)
        assertThat(buyMatch.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(buyMatch.date).isEqualTo(date)
        assertThat(buyMatch.adhoc).isFalse()

        assertThat(sellMatch.hash).isEqualTo(orders.sellOrder.hash)
        assertThat(sellMatch.fill).isEqualTo(EthUInt256.of(price))
        assertThat(sellMatch.side).isEqualTo(OrderSide.RIGHT)
        assertThat(sellMatch.maker).isEqualTo(Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9"))
        assertThat(sellMatch.taker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(sellMatch.take.type).isInstanceOf(Erc20AssetType::class.java)
        assertThat((sellMatch.take.type as Erc20AssetType).token).isEqualTo(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab"))
        assertThat(sellMatch.take.value).isEqualTo(EthUInt256.of(price))
        assertThat(sellMatch.make.type).isInstanceOf(NftAssetType::class.java)
        assertThat((sellMatch.make.type as NftAssetType).token).isEqualTo(Address.apply("0x509fd4cdaa29be7b1fad251d8ea0fca2ca91eb60"))
        assertThat((sellMatch.make.type as NftAssetType).tokenId).isEqualTo(EthUInt256.of(110711))
        assertThat(sellMatch.make.value).isEqualTo(EthUInt256.ONE)
        assertThat(sellMatch.source).isEqualTo(HistorySource.OPEN_SEA)
        assertThat(sellMatch.date).isEqualTo(date)
        assertThat(sellMatch.adhoc).isTrue()
    }

    @Test
    fun `should parse cancel sell order`() = runBlocking<Unit> {
        val input = "0xa8a41c700000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c01073000000000000000000000000509fd4cdaa29be7b1fad251d8ea0fca2ca91eb600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001d882cb9b2080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006112483d0000000000000000000000000000000000000000000000000000000000000000cffccf577da3209cfef62013432a7e215160a6bc3a77f38ffeeb4c1b96e7b38f0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034000000000000000000000000000000000000000000000000000000000000003e00000000000000000000000000000000000000000000000000000000000000480000000000000000000000000000000000000000000000000000000000000001c4232ed041f058749aaa45551c1c2a3efd4754065deaf194795159e5fd1335d477f4111bcaf88bfdd7dca31c204c0db6788230cfb5eb6a6ed3f2c56c35bf7e675000000000000000000000000000000000000000000000000000000000000006423b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val order = parser.parseCancelOrder(input)
        assertThat(order).isNotNull

        assertThat(order!!.exchange).isEqualTo(Address.apply("0x5206e78b21ce315ce284fb24cf05e0585a93b1d9"))
        assertThat(order.maker).isEqualTo(Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"))
        assertThat(order.taker).isEqualTo(Address.apply("0x0000000000000000000000000000000000000000"))
        assertThat(order.makerRelayerFee).isEqualTo(BigInteger.valueOf(250))
        assertThat(order.takerRelayerFee).isEqualTo(BigInteger.ZERO)
        assertThat(order.makerProtocolFee).isEqualTo(BigInteger.ZERO)
        assertThat(order.takerProtocolFee).isEqualTo(BigInteger.ZERO)
        assertThat(order.feeRecipient).isEqualTo(Address.apply("0x5b3256965e7c3cf26e11fcaf296dfc8807c01073"))
        assertThat(order.feeMethod).isEqualTo(OpenSeaOrderFeeMethod.SPLIT_FEE)
        assertThat(order.side).isEqualTo(OpenSeaOrderSide.SELL)
        assertThat(order.howToCall).isEqualTo(OpenSeaOrderHowToCall.CALL)
        assertThat(order.saleKind).isEqualTo(OpenSeaOrderSaleKind.FIXED_PRICE)
        assertThat(order.target).isEqualTo(Address.apply("0x509fd4cdaa29be7b1fad251d8ea0fca2ca91eb60"))
        assertThat(order.callData).isEqualTo(Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(order.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(order.staticTarget).isEqualTo(Address.apply("0x0000000000000000000000000000000000000000"))
        assertThat(order.staticExtraData).isEqualTo(Binary.apply())
        assertThat(order.paymentToken).isEqualTo(Address.apply("0x0000000000000000000000000000000000000000"))
        assertThat(order.basePrice).isEqualTo(BigInteger.valueOf(133000000000000000))
        assertThat(order.extra).isEqualTo(BigInteger.ZERO)
        assertThat(order.listingTime).isEqualTo(BigInteger.valueOf(1628588093))
        assertThat(order.expirationTime).isEqualTo(BigInteger.ZERO)
        assertThat(order.salt).isEqualTo(BigInteger("94075436137300584109920163749311521813551693230538482170487180241919466255247"))
    }

    @Test
    fun `should parse valid cancel safely`() = runBlocking<Unit> {
        val input = "0xa8a41c700000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c01073000000000000000000000000509fd4cdaa29be7b1fad251d8ea0fca2ca91eb600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001d882cb9b2080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006112483d0000000000000000000000000000000000000000000000000000000000000000cffccf577da3209cfef62013432a7e215160a6bc3a77f38ffeeb4c1b96e7b38f0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034000000000000000000000000000000000000000000000000000000000000003e00000000000000000000000000000000000000000000000000000000000000480000000000000000000000000000000000000000000000000000000000000001c4232ed041f058749aaa45551c1c2a3efd4754065deaf194795159e5fd1335d477f4111bcaf88bfdd7dca31c204c0db6788230cfb5eb6a6ed3f2c56c35bf7e675000000000000000000000000000000000000000000000000000000000000006423b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val order = parser.parseCancelOrder(input)
        assertThat(order).isNotNull
    }

    @Test
    fun `should parse invalid cancel safely`() = runBlocking<Unit> {
        val input = "0x18a41c700000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b600000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c01073000000000000000000000000509fd4cdaa29be7b1fad251d8ea0fca2ca91eb600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000fa00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001d882cb9b2080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006112483d0000000000000000000000000000000000000000000000000000000000000000cffccf577da3209cfef62013432a7e215160a6bc3a77f38ffeeb4c1b96e7b38f0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034000000000000000000000000000000000000000000000000000000000000003e00000000000000000000000000000000000000000000000000000000000000480000000000000000000000000000000000000000000000000000000000000001c4232ed041f058749aaa45551c1c2a3efd4754065deaf194795159e5fd1335d477f4111bcaf88bfdd7dca31c204c0db6788230cfb5eb6a6ed3f2c56c35bf7e675000000000000000000000000000000000000000000000000000000000000006423b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val order = parser.parseCancelOrder(input)
        assertThat(order).isNull()
    }

    @Test
    fun `should parse valid order match safely`() = runBlocking<Unit> {
        val input = "0xab834bab0000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d900000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000000000000000000000000000000000000000000000000000000000000000000004a6a5703a9796630e9fa04f5ecaf730065a7b827000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005206e78b21ce315ce284fb24cf05e0585a93b1d90000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b900000000000000000000000000000000000000000000000000000000000000000000000000000000000000005b3256965e7c3cf26e11fcaf296dfc8807c010730000000000000000000000004a6a5703a9796630e9fa04f5ecaf730065a7b8270000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004e200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003311fc80a5700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102e03f00000000000000000000000000000000000000000000000000000000000000003f9b23e00ccc19a1e6399b0bf5df8a7cac4df96c79b1fdf548e85f00c13be3ea00000000000000000000000000000000000000000000000000000000000004e200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003311fc80a5700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006102dff100000000000000000000000000000000000000000000000000000000000000006db950761e734348580ff301dadd346aadd7960399a7390f39cf0e6f66159b590000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006a0000000000000000000000000000000000000000000000000000000000000074000000000000000000000000000000000000000000000000000000000000007e0000000000000000000000000000000000000000000000000000000000000088000000000000000000000000000000000000000000000000000000000000009200000000000000000000000000000000000000000000000000000000000000940000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000000000000000000000000000000000000000001b8868021b1bb1de3b41ae8325a9934dfa004eabea54b4c1b76e353406abf1bea2644d1917aaed64d531c5ca7b47756cd91f461de089592d18fdb7fc176baf5c838868021b1bb1de3b41ae8325a9934dfa004eabea54b4c1b76e353406abf1bea2644d1917aaed64d531c5ca7b47756cd91f461de089592d18fdb7fc176baf5c830000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006423b872dd0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b90000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006400000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".toBinary()
        val order = parser.parseMatchedOrders(input)
        assertThat(order).isNotNull
    }
}
