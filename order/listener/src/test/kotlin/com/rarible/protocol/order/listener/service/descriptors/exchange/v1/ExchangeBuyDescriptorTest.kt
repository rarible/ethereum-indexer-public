package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.common.deprecated.TransferProxyForDeprecated
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.v1.BuyEvent
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v1.state.ExchangeStateV1
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.misc.toBinary
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.Order.Companion.legacyMessage
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.core.service.SignUtils
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.setField
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
@FlowPreview
class ExchangeBuyDescriptorTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var exchangeBuyDescriptor: ExchangeBuyDescriptor

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    @Test
    fun convert() = runBlocking {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val owner = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val buyerPrivateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val buyer = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            buyerPrivateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val beneficiary = Address.THREE()

        val buyerFeeSignerPrivateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
        val buyerFeeSigner = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            buyerFeeSignerPrivateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val token = TestERC1155.deployAndWait(owner, poller, "ipfs:/").block()!!
        val tokenId = BigInteger.ONE

        val buyToken = TestERC20.deployAndWait(sender, poller, "TEST", "TST").block()!!

        val salt = BigInteger.TEN

        val state = ExchangeStateV1.deployAndWait(owner, poller).block()!!
        val proxy = TransferProxy.deployAndWait(owner, poller).block()!!
        val proxyForDeprecated = TransferProxyForDeprecated.deployAndWait(owner, poller).block()!!
        val erc20Proxy = ERC20TransferProxy.deployAndWait(owner, poller).block()!!
        val sale = ExchangeV1.deployAndWait(
            owner,
            poller,
            proxy.address(),
            proxyForDeprecated.address(),
            erc20Proxy.address(),
            state.address(),
            Address.ZERO(),
            beneficiary,
            buyerFeeSigner.from()
        ).block()!!

        state.addOperator(sale.address())
            .execute().verifySuccess()
        proxy.addOperator(sale.address())
            .execute().verifySuccess()
        proxyForDeprecated.addOperator(sale.address())
            .execute().verifySuccess()
        erc20Proxy.addOperator(sale.address())
            .execute().verifySuccess()
        token.setApprovalForAll(proxy.address(), true)
            .execute().verifySuccess()
        buyToken.approve(erc20Proxy.address(), BigInteger.TEN.pow(30))
            .withSender(buyer)
            .execute().verifySuccess()

        setField(exchangeBuyDescriptor, "addresses", listOf(sale.address()))
        setField(prepareTxService, "privateKey", buyerFeeSignerPrivateKey)

        val ownerHas = BigInteger.TEN
        token.mint(owner.from(), tokenId, ownerHas, ByteArray(0)).execute().verifySuccess()

        val buyerHas = 20.toBigInteger()
        buyToken.mint(buyer.from(), buyerHas).withSender(buyer).execute().verifySuccess()

        val orderVersionLeft = OrderVersion(
            maker = owner.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token.address(), EthUInt256.of(tokenId)), EthUInt256.TEN),
            take = Asset(Erc20AssetType(buyToken.address()), EthUInt256.of(5)),
            type = OrderType.RARIBLE_V1,
            salt = EthUInt256.of(salt),
            start = null,
            end = null,
            data = OrderDataLegacy(fee = 0),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            signature = null
        ).let {
            it.copy(
                signature = SignUtils.sign(privateKey, it.legacyMessage()).toBinary()
            )
        }

        val orderLeft = orderUpdateService.save(orderVersionLeft)

        val buyerFee = 500

        val amount = 2.toBigInteger()

        val prepared = prepareTxService.prepareTransaction(
            orderLeft,
            PrepareOrderTxFormDto(
                buyer.from(), amount, emptyList(), listOf(
                    PartDto(Address.ZERO(), buyerFee)
                )
            )
        )
        buyer.sendTransaction(
            Transaction(
                sale.address(),
                buyer.from(),
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                prepared.transaction.data,
                null
            )
        ).verifySuccess()

        assertEquals(token.balanceOf(owner.from(), tokenId).call().block(), ownerHas - amount)
        assertEquals(token.balanceOf(buyer.from(), tokenId).call().block(), amount)
        val buyAmount = orderLeft.take.value.value * amount / orderLeft.make.value.value
        assertEquals(buyToken.balanceOf(owner.from()).call().block(), buyAmount)
        assertEquals(buyToken.balanceOf(buyer.from()).call().block(), buyerHas - buyAmount)

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.BUY).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val buy = items.map { it.data }.filterIsInstance<OrderSideMatch>().find { it.maker == owner.from() }!!

            val failMessage = "result: $buy"

            val makeTest = Asset(
                type = Erc1155AssetType(token.address(), EthUInt256(tokenId)),
                value = EthUInt256(amount)
            )
            assertThat(buy.make)
                .isEqualTo(makeTest).withFailMessage(failMessage)

            assertThat(buy.taker)
                .isEqualTo(buyer.from()).withFailMessage(failMessage)
            assertThat(buy.take.type)
                .isEqualTo(Erc20AssetType(buyToken.address())).withFailMessage(failMessage)

            checkActivityWasPublished(orderLeft, BuyEvent.id(), OrderActivityMatchDto::class.java)
        }
    }
}