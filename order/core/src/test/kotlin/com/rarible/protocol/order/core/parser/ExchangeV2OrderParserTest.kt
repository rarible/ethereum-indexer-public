package com.rarible.protocol.order.core.parser

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.randomErc1155
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomPart
import com.rarible.protocol.order.core.data.randomPartDto
import com.rarible.protocol.order.core.misc.fixV
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.misc.toSignatureData
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.invert
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scala.Tuple4
import scalether.domain.Address
import java.math.BigInteger

class ExchangeV2OrderParserTest {

    private lateinit var eip712Domain: EIP712Domain

    @BeforeEach
    fun setup() {
        eip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = randomAddress()
        )
    }

    @Test
    fun `should get parser simple sell order`() = runBlocking<Unit> {
        val newKeys = generateNewKeys()

        val order = createOrder().copy(
            maker = newKeys.address,
            make = randomErc721(),
            take = randomErc20(EthUInt256.of(5)),
            data = OrderRaribleV2DataV1(
                payouts = listOf(randomPart(), randomPart()),
                originFees = listOf(randomPart(), randomPart())
            )
        )
        val signature = eip712Domain.hashToSign(Order.hash(order)).sign(newKeys.privateKey)

        val maker = randomAddress()

        val prepareOrderTxFormDto = PrepareOrderTxFormDto(
            maker = randomAddress(),
            amount = EthUInt256.ONE.value,
            payouts = listOf(randomPartDto(), randomPartDto()),
            originFees = listOf(randomPartDto(), randomPartDto()),
        )

        val orderRight = order.invert(
            maker = maker,
            amount = EthUInt256.ONE.value,
            newData = order.data
        )

        val data = ExchangeV2.matchOrdersSignature().encode(
            Tuple4(
                order.forTx(false),
                signature.toSignatureData().fixV().toBinary().bytes(),
                orderRight.forTx(false),
                ByteArray(0)
            )
        )

        val parsedOrders = ExchangeV2OrderParser.parseMatchedOrders(data)
        assertThat(parsedOrders.left.makeAssetType).isEqualTo(order.make.type)
        assertThat(parsedOrders.left.takeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.left.data).isEqualTo(order.data)

        assertThat(parsedOrders.right.makeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.right.takeAssetType).isEqualTo(order.make.type)
    }

    @Test
    fun `should get parser simple bid order`() = runBlocking<Unit> {
        val newKeys = generateNewKeys()

        val order = createOrder().copy(
            maker = newKeys.address,
            make = randomErc20(EthUInt256.of(5)),
            take = randomErc1155(EthUInt256.of(5)),
            data = OrderRaribleV2DataV1(
                payouts = listOf(randomPart(), randomPart()),
                originFees = listOf(randomPart(), randomPart())
            )
        )

        val signature = eip712Domain.hashToSign(Order.hash(order)).sign(newKeys.privateKey)
        val maker = randomAddress()

        val orderRight = order.invert(
            maker = maker,
            amount = EthUInt256.ONE.value,
            newData = order.data
        )

        val data = ExchangeV2.matchOrdersSignature().encode(
            Tuple4(
                order.forTx(false),
                signature.toSignatureData().fixV().toBinary().bytes(),
                orderRight.forTx(false),
                ByteArray(0)
            )
        )

        val parsedOrders = ExchangeV2OrderParser.parseMatchedOrders(data)
        assertThat(parsedOrders.left.makeAssetType).isEqualTo(order.make.type)
        assertThat(parsedOrders.left.takeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.left.data).isEqualTo(order.data)

        assertThat(parsedOrders.right.makeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.right.takeAssetType).isEqualTo(order.make.type)
    }

    @Test
    fun `parse direct bid - ok`() = runBlocking<Unit> {
        val marchedOrder = ExchangeV2OrderParser.parseMatchedOrders(Binary.apply(directBidTransactionInput))
        println(marchedOrder)

        val buy = marchedOrder.left
        val sell = marchedOrder.right

        // BUY order

        assertThat(buy.hash).isEqualTo(Word.apply("0x8a77312c261311497116be3df125730f6a380f98c02231ffa2284f7f13bef6ea"))
        assertThat(buy.maker).isEqualTo(Address.apply("0x0d1d4e623d10f9fba5db95830f7d3839406c6af2"))
        assertThat(buy.salt.value).isEqualTo(BigInteger.ONE)
        assertThat(buy.isMakeFillOrder).isEqualTo(false)
        assertThat(buy.marketplaceMarker)
            .isEqualTo(Word.apply("0x68619b8adb206de04f676007b2437f99ff6129b672495a6951499c6c56bc2f14"))
        assertThat(buy.data).isInstanceOf(OrderRaribleV2DataV3Buy::class.java)

        assertThat(buy.originFees).hasSize(1)
        assertThat(buy.originFees!![0].account).isEqualTo(Address.apply("0x5aeda56215b167893e80b4fe645ba6d5bab767de"))
        assertThat(buy.originFees!![0].value.value).isEqualTo(300)

        val buyMake = buy.makeAssetType as Erc20AssetType
        val buyTake = buy.takeAssetType as Erc721AssetType

        assertThat(buyMake.token).isEqualTo(Address.apply("0x432adf73ac67ff0810eae7dec53dbf077f228a6e"))
        assertThat(buyTake.token).isEqualTo(Address.apply("0xaa862ddac09f6736a61e1124040fd883a6533c19"))
        assertThat(buyTake.tokenId.value).isEqualTo(12345)

        // SELL order

        assertThat(sell.hash).isEqualTo(
            Word.apply("0x067307dbaeddb008c4c096286f3661d428f20edd22a2ef766f4fd2d28b8c7b2c")
        )
        assertThat(sell.maker).isEqualTo(Address.apply("0x0000000000000000000000000000000000000000"))
        assertThat(sell.salt.value).isEqualTo(BigInteger.ZERO)
        assertThat(sell.isMakeFillOrder).isEqualTo(true)
        assertThat(sell.marketplaceMarker)
            .isEqualTo(Word.apply("0x68619b8adb206de04f676007b2437f99ff6129b672495a6951499c6c56bc2f13"))
        assertThat(sell.data).isInstanceOf(OrderRaribleV2DataV3Sell::class.java)

        assertThat(sell.originFees).isEmpty()
        assertThat(sell.makeAssetType).isEqualTo(buyTake)
        assertThat(sell.takeAssetType).isEqualTo(buyMake)
    }

    @Test
    fun `parse direct purchase - ok`() = runBlocking<Unit> {
        val marchedOrder = ExchangeV2OrderParser.parseMatchedOrders(Binary.apply(directPurchaseTransaction))
        println(marchedOrder)

        val sell = marchedOrder.left
        val buy = marchedOrder.right

        // SELL order

        assertThat(sell.hash).isEqualTo(
            Word.apply("0x8711535b5062a4eb28b49ed4501f2fb79ced29addab96a14e849b4f92bc6eb68")
        )
        assertThat(sell.maker).isEqualTo(Address.apply("0xedddf4dc3c69de582d4f04128a06419b03801f25"))
        assertThat(sell.salt)
            .isEqualTo(EthUInt256.of("0xb5372f6edfc6ce1c715d6d267f217a0dab9773f3f5a4ff1fb459c32ea6f59451"))
        assertThat(sell.isMakeFillOrder).isEqualTo(true)
        assertThat(sell.marketplaceMarker).isNull()
        assertThat(sell.data).isInstanceOf(OrderRaribleV2DataV2::class.java)

        assertThat(sell.originFees).hasSize(1)
        assertThat(sell.originFees!![0].account).isEqualTo(Address.apply("0x1cf0df2a5a20cd61d68d4489eebbf85b8d39e18a"))
        assertThat(sell.originFees!![0].value.value).isEqualTo(100)

        val sellMake = sell.makeAssetType as Erc721AssetType
        val sellTake = sell.takeAssetType as EthAssetType

        assertThat(sellMake.token).isEqualTo(Address.apply("0xe116d562ab8971e6a3891fed606fb58adc602dc8"))
        assertThat(sellMake.tokenId.value).isEqualTo(1)

        // BUY order

        assertThat(buy.hash).isEqualTo(Word.apply("0xe74715ddc773b90db66de9a6b6f79c4a04ed939b7dfac1b0b53732d2bad1e066"))
        assertThat(buy.maker).isEqualTo(Address.apply("0x0000000000000000000000000000000000000000"))
        assertThat(buy.salt.value).isEqualTo(BigInteger.ZERO)
        assertThat(buy.isMakeFillOrder).isEqualTo(false)
        assertThat(buy.marketplaceMarker).isNull()
        assertThat(buy.data).isInstanceOf(OrderRaribleV2DataV2::class.java)

        assertThat(buy.originFees).isEqualTo(sell.originFees)

        assertThat(buy.makeAssetType).isEqualTo(sellTake)
        assertThat(buy.takeAssetType).isEqualTo(sellMake)
    }

    private fun generateNewKeys(): NewKeys {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    fun Word.sign(privateKey: BigInteger): Binary {
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        return Sign.signMessageHash(bytes(), publicKey, privateKey).toBinary()
    }

    private val directPurchaseTransaction = "0x0d5f7d35000000000000000000000000000000000000000000000000000000000000002" +
        "0000000000000000000000000edddf4dc3c69de582d4f04128a06419b03801f250000000000000000000000000000000000000000000" +
        "00000000000000000000173ad21460000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000001e000000000000000000000000000000000000000000000000000005af3107a4000000" +
        "0000000000000000000000000000000000000000000000000000000000000b5372f6edfc6ce1c715d6d267f217a0dab9773f3f5a4ff1" +
        "fb459c32ea6f594510000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000063f7068823d235ef000000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000240000000000000000000000000000000000000000000000000000" +
        "000000000036000000000000000000000000000000000000000000000000000005af3107a40000000000000000000000000000000000" +
        "00000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000003e000000000000" +
        "00000000000000000000000000000000000000000000000000040000000000000000000000000e116d562ab8971e6a3891fed606fb58" +
        "adc602dc8000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000" +
        "000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000020000000000000000" +
        "000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000" +
        "000800000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000" +
        "000001cf0df2a5a20cd61d68d4489eebbf85b8d39e18a000000000000000000000000000000000000000000000000000000000000006" +
        "400000000000000000000000000000000000000000000000000000000000000412c2e0cf0aaa3da94994edd020f0fe958a876d50b7b5" +
        "519164a56ddeb35233654640f4d892dab38183a6a3fc84738d3c7029ec6ef2ae78fb30f4bcc0525ab7abe1c000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000" +
        "000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000" +
        "000000000000000600000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000010000000000000000000000001cf0df2a5a20cd61d68d4489eeb" +
        "bf85b8d39e18a00000000000000000000000000000000000000000000000000000000000000640000000000000000000000000000000" +
        "0000000000000000109616c6c64617461"

    private val directBidTransactionInput = "0x67d49a3b00000000000000000000000000000000000000000000000000000000000000" +
        "200000000000000000000000000d1d4e623d10f9fba5db95830f7d3839406c6af2000000000000000000000000000000000000000000" +
        "000000000000000000000173ad2146000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000001e000000000000000000000000000000000000000000000000000000000000003e800" +
        "0000000000000000000000432adf73ac67ff0810eae7dec53dbf077f228a6e0000000000000000000000000000000000000000000000" +
        "000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000001b18cdf600000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000024000000000000000000000000000000000000000000000000000" +
        "000000000002e000000000000000000000000000000000000000000000000000000000000003e8000000000000000000000000000000" +
        "000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000003600000000000" +
        "000000000000000000000000000000000000000000000000000040000000000000000000000000aa862ddac09f6736a61e1124040fd8" +
        "83a6533c1900000000000000000000000000000000000000000000000000000000000030390000000000000000000000000000000000" +
        "000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "000000012c5aeda56215b167893e80b4fe645ba6d5bab767de0000000000000000000000000000000000000000000000000000000000" +
        "00000068619b8adb206de04f676007b2437f99ff6129b672495a6951499c6c56bc2f1400000000000000000000000000000000000000" +
        "000000000000000000000000410e8dacccfd7e7655a3ca75765e760116a69845cecbb84e5186653a7886b262c1133f43b30d554ebd74" +
        "5893b10e5dbbce89b3d0ab5aca85d80184eb8b3b7d98761b000000000000000000000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
        "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003e868" +
        "619b8adb206de04f676007b2437f99ff6129b672495a6951499c6c56bc2f13"

}