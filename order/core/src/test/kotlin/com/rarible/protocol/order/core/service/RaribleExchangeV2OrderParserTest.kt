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