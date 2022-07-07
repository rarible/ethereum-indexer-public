package com.rarible.protocol.order.core.service

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.*
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
internal class RaribleExchangeV2OrderParserTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var exchangeContractAddresses: OrderIndexerProperties

    @Autowired
    private lateinit var raribleExchangeV2OrderParser: RaribleExchangeV2OrderParser

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    private lateinit var eip712Domain: EIP712Domain

    @BeforeEach
    fun setup() {
        eip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = exchangeContractAddresses.exchangeContractAddresses.v2
        )
    }

    @Test
    fun `execute meta transaction parse`() = runBlocking<Unit> {
        // transaction input from https://polygon-mainnet.g.alchemyapi.io,
        // transaction hash - "0xcf9253e4b65b7e94e0ce867dbaf6318f175decead413657ded9738cca53405d8"
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

        val marchedOrder = raribleExchangeV2OrderParser.parseOrders(Binary.apply(transactionInput))
        println(marchedOrder)
        assertThat(marchedOrder::class).isEqualTo(RaribleMatchedOrders::class)
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

        val prepareOrderTxFormDto = PrepareOrderTxFormDto(
            maker = randomAddress(),
            amount = EthUInt256.ONE.value,
            payouts = listOf(randomPartDto(), randomPartDto()),
            originFees = listOf(randomPartDto(), randomPartDto()),
        )
        val result = prepareTxService.prepareTxForV2(order.copy(signature = signature), prepareOrderTxFormDto)
        val parsedOrders = raribleExchangeV2OrderParser.parseMatchedOrders(result.transaction.data)
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

        val prepareOrderTxFormDto = PrepareOrderTxFormDto(
            maker = randomAddress(),
            amount = EthUInt256.ONE.value,
            payouts = listOf(randomPartDto(), randomPartDto()),
            originFees = listOf(randomPartDto(), randomPartDto()),
        )
        val result = prepareTxService.prepareTxForV2(order.copy(signature = signature), prepareOrderTxFormDto)
        val parsedOrders = raribleExchangeV2OrderParser.parseMatchedOrders(result.transaction.data)
        assertThat(parsedOrders.left.makeAssetType).isEqualTo(order.make.type)
        assertThat(parsedOrders.left.takeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.left.data).isEqualTo(order.data)

        assertThat(parsedOrders.right.makeAssetType).isEqualTo(order.take.type)
        assertThat(parsedOrders.right.takeAssetType).isEqualTo(order.make.type)
    }

    @Test
    fun `should parse order data v3 sell`() {
        val expected = createOrderRaribleV1DataV3Sell()
        val model = raribleExchangeV2OrderParser.convertOrderData(expected.version.ethDataType!!, expected.toEthereum())
        assertThat(model).isInstanceOf(OrderRaribleV2DataV3Sell::class.java)
        with(model as OrderRaribleV2DataV3Sell) {
            assertThat(payout!!).isEqualTo(expected.payout)
            assertThat(originFeeFirst!!).isEqualTo(expected.originFeeFirst)
            assertThat(originFeeSecond!!).isEqualTo(expected.originFeeSecond)
            assertThat(maxFeesBasePoint).isEqualTo(expected.maxFeesBasePoint)
            assertThat(marketplaceMarker!!).isEqualTo(expected.marketplaceMarker)
        }
    }

    @Test
    fun `should parse order data v3 buy`() {
        val expected = createOrderRaribleV1DataV3Buy()
        val model = raribleExchangeV2OrderParser.convertOrderData(expected.version.ethDataType!!, expected.toEthereum())
        assertThat(model).isInstanceOf(OrderRaribleV2DataV3Buy::class.java)
        with(model as OrderRaribleV2DataV3Buy) {
            assertThat(payout!!).isEqualTo(expected.payout)
            assertThat(originFeeFirst!!).isEqualTo(expected.originFeeFirst)
            assertThat(originFeeSecond!!).isEqualTo(expected.originFeeSecond)
            assertThat(marketplaceMarker!!).isEqualTo(expected.marketplaceMarker)
        }
    }

    protected fun generateNewKeys(): NewKeys {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    fun Word.sign(privateKey: BigInteger): Binary {
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        return Sign.signMessageHash(bytes(), publicKey, privateKey).toBinary()
    }
}