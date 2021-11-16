package com.rarible.protocol.order.api.controller

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v1.state.ExchangeStateV1
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.contracts.royalties.TestRoyaltiesProvider
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.misc.setField
import com.rarible.protocol.order.api.service.order.AbstractOrderIt
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.util.Hex
import java.math.BigInteger

@IntegrationTest
class PrepareOrderMatchTransactionTest : AbstractOrderIt() {
    @Test
    fun `should prepare v1 tx`() = runBlocking<Unit> {
        val realBuyer = AddressFactory.create()

        val (seller, sellerKey, buyer, _, erc721) = prepare()

        val order = createOrder(seller).copy(
            type = OrderType.RARIBLE_V1,
            data = OrderDataLegacy(1),
            make = Asset(
                type = Erc721AssetType(erc721.address(), EthUInt256.ONE),
                value = EthUInt256.ONE
            ),
            take = Asset(
                type = EthAssetType,
                value = EthUInt256.of(100)
            )
        )
        val created = orderClient.upsertOrder(order.toForm(sellerKey)).awaitFirst()
        val prepared = orderClient.prepareOrderTransaction(
            created.hash.toString(), PrepareOrderTxFormDto(
                maker = buyer.from(),
                amount = BigInteger.ONE,
                payouts = listOf(PartDto(realBuyer, 10000)),
                originFees = emptyList()
            )
        ).awaitFirst()

        buyer.sendTransaction(
            Transaction(
                prepared.transaction.to,
                null,
                500000.toBigInteger(),
                BigInteger.ZERO,
                prepared.asset.value,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(erc721.ownerOf(BigInteger.ONE).call().awaitFirst())
            .isEqualTo(realBuyer)
    }

    @Test
    fun `should prepare v2 tx`() = runBlocking<Unit> {
        val (_, _, realBuyer) = generateNewKeys()

        val (seller, sellerKey, buyer, _, erc721) = prepare()

        val order = createOrder(seller).copy(
            type = OrderType.RARIBLE_V2,
            data = OrderRaribleV2DataV1(listOf(), listOf()),
            make = Asset(
                type = Erc721AssetType(erc721.address(), EthUInt256.ONE),
                value = EthUInt256.ONE
            ),
            take = Asset(
                type = EthAssetType,
                value = EthUInt256.of(100)
            )
        )
        val created = orderClient.upsertOrder(order.toForm(sellerKey)).awaitFirst()
        val prepared = orderClient.prepareOrderTransaction(
            created.hash.toString(), PrepareOrderTxFormDto(
                maker = buyer.from(),
                amount = BigInteger.ONE,
                payouts = listOf(PartDto(realBuyer, 10000)),
                originFees = emptyList()
            )
        ).awaitFirst()

        buyer.sendTransaction(
            Transaction(
                prepared.transaction.to,
                null,
                500000.toBigInteger(),
                BigInteger.ZERO,
                prepared.asset.value,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertThat(erc721.ownerOf(BigInteger.ONE).call().awaitFirst())
            .isEqualTo(realBuyer)
    }

    private suspend fun prepare(): PrepareResult {
        val hasEthSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt(Hex.toBytes("26250bb39160076f030517503da31e11aca80060d14f84ebdaced666efb89e21")),
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val (deployerKey, _, deployer) = generateNewKeys()
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            deployerKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val transferProxy = TransferProxy.deployAndWait(sender, poller).awaitFirst()
        val state = ExchangeStateV1.deployAndWait(sender, poller).awaitFirst()

        val beneficiary = AddressFactory.create()
        val v1 = ExchangeV1.deployAndWait(
            sender,
            poller,
            transferProxy.address(),
            Address.ZERO(),
            Address.ZERO(),
            state.address(),
            Address.ZERO(),
            beneficiary,
            deployer
        ).awaitFirst()
        transferProxy.addOperator(v1.address()).execute().verifySuccess()
        state.addOperator(v1.address()).execute().verifySuccess()
        logger.info("deployed v1: ${v1.address()}")

        val v2 = ExchangeV2.deployAndWait(sender, poller).awaitFirst()
        setV2Address(v2.address())
        val royaltiesProvider = TestRoyaltiesProvider.deployAndWait(sender, poller).awaitFirst()
        v2.__ExchangeV2_init(transferProxy.address(), Address.ZERO(), BigInteger.ZERO, beneficiary, royaltiesProvider.address()).execute().verifySuccess()
        transferProxy.addOperator(v2.address()).execute().verifySuccess()
        setField(
            prepareTxService,
            "exchangeContractAddresses",
            OrderIndexerProperties.ExchangeContractAddresses(
                v1 = v1.address(),
                v1Old = null,
                v2 = v2.address(),
                openSeaV1 = AddressFactory.create(),
                cryptoPunks = AddressFactory.create()
            )
        )
        setField(
            prepareTxService,
            "privateKey",
            deployerKey
        )
        logger.info("deployed v2: ${v2.address()}")

        val erc721 = TestERC721.deployAndWait(sender, poller, "test", "test").awaitFirst()
        logger.info("deployed erc721: ${erc721.address()}")

        val (sellerKey, _, seller) = generateNewKeys()
        val sellerSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            sellerKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        erc721.setApprovalForAll(transferProxy.address(), true).withSender(sellerSender).execute().verifySuccess()
        erc721.mint(seller, BigInteger.ONE, "uri").execute().verifySuccess()

        val buyerKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val buyer = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            buyerKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        hasEthSender.sendTransaction(
            Transaction(
                buyer.from(),
                null,
                100000.toBigInteger(),
                BigInteger.ZERO,
                1000.toBigInteger(),
                Binary.empty(),
                null
            )
        ).verifySuccess()

        return PrepareResult(
            seller = seller,
            sellerKey = sellerKey,
            buyer = buyer,
            buyerKey = buyerKey,
            erc721 = erc721
        )
    }

    private data class PrepareResult(
        val seller: Address,
        val sellerKey: BigInteger,
        val buyer: MonoSigningTransactionSender,
        val buyerKey: BigInteger,
        val erc721: TestERC721
    )
}
