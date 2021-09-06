package com.rarible.protocol.order.api.controller

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.common.deprecated.TransferProxyForDeprecated
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v1.state.ExchangeStateV1
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.misc.setField
import com.rarible.protocol.order.api.service.pending.PendingTransactionService
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.asset.AssetTypeService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple3
import scala.Tuple4
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.domain.response.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
@Import(TransactionControllerFt.TestAssetTypeServiceConfiguration::class)
class TransactionControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var assetTypeService: AssetTypeService

    @Autowired
    lateinit var pendingTransactionService: PendingTransactionService

    @Autowired
    lateinit var exchangeHistoryRepository: ExchangeHistoryRepository

    companion object {
        lateinit var userSender: MonoSigningTransactionSender
        lateinit var sender: MonoSigningTransactionSender

        lateinit var token: TestERC1155
        lateinit var buyToken: TestERC1155

        private val salt = BigInteger.TEN
        private val tokenId = BigInteger.ONE
        private val buyTokenId = BigInteger.TEN
    }

    @BeforeEach
    fun before() {
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        userSender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt("0x0a2853fac2c0a03f463f04c4567839473c93f3307da459132b7dd1ca633c0e16"),
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        token = TestERC1155.deployAndWait(userSender, poller, "ipfs:/").block()!!
        buyToken = TestERC1155.deployAndWait(userSender, poller, "ipfs:/").block()!!
    }

    @Test
    fun `should create pending transaction for cancel`() = runBlocking<Unit> {
        val beneficiary = Address.THREE()

        val buyerFeeSignerPrivateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val buyerFeeSigner = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            buyerFeeSignerPrivateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )

        val state = ExchangeStateV1.deployAndWait(userSender, poller).block()!!
        val proxy = TransferProxy.deployAndWait(userSender, poller).block()!!
        val proxyForDeprecated = TransferProxyForDeprecated.deployAndWait(userSender, poller).block()!!
        val erc20Proxy = ERC20TransferProxy.deployAndWait(userSender, poller).block()!!
        val sale = ExchangeV1.deployAndWait(userSender, poller, proxy.address(), proxyForDeprecated.address(), erc20Proxy.address(), state.address(), Address.ZERO(), beneficiary, buyerFeeSigner.from()).block()!!

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

        val orderKey = Tuple4(sender.from(), salt, Tuple3(token.address(), tokenId, LegacyAssetTypeClass.ERC1155.value), Tuple3(buyToken.address(), buyTokenId, LegacyAssetTypeClass.ERC1155.value))

        val receipt = sale.cancel(orderKey).withSender(sender).execute().verifySuccess()

        val makeAssetType = Erc1155AssetType(
            token.address(),
            EthUInt256.of(tokenId)
        )
        val takeAssetType = Erc1155AssetType(
            buyToken.address(),
            EthUInt256.of(buyTokenId)
        )
        val order = Order(
            maker = sender.from(),
            taker = AddressFactory.create(),
            make = Asset(
                type = makeAssetType,
                value = EthUInt256.TEN
            ),
            take = Asset(
                type = takeAssetType,
                value = EthUInt256.TEN
            ),
            type = OrderType.RARIBLE_V1,
            fill = EthUInt256.TEN,
            cancelled = false,
            makeStock = EthUInt256.TEN,
            salt = EthUInt256.of(salt),
            data = OrderDataLegacy(0),
            start = null,
            end = null,
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )

        setField(pendingTransactionService, "exchangeContracts", hashSetOf(sale.address()))

        orderRepository.save(order)

        coEvery { assetTypeService.toAssetType(eq(makeAssetType.token), eq(makeAssetType.tokenId)) } returns makeAssetType
        coEvery { assetTypeService.toAssetType(eq(takeAssetType.token), eq(takeAssetType.tokenId)) } returns takeAssetType

        processTransaction(receipt)

        val history = exchangeHistoryRepository.findLogEvents(order.hash, null).collectList().awaitFirst()
        assertThat(history).hasSize(1)
        assertThat(history.single().status).isEqualTo(LogEventStatus.PENDING)

        val savedOrder = orderRepository.findById(order.hash)
        assertThat(savedOrder?.pending).hasSize(1)
        assertThat(savedOrder?.pending?.single()).isInstanceOf(OrderCancel::class.java)
    }

    private suspend fun processTransaction(receipt: TransactionReceipt) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val transactions = transactionApi.createOrderPendingTransaction(tx.toRequest()).collectList().block()

        assertThat(transactions).hasSize(1)

        with(transactions.single()) {
            assertThat(transactionHash).isEqualTo(tx.hash())
            assertThat(address).isEqualTo(tx.to())
            assertThat(status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    private fun Transaction.toRequest() = CreateTransactionRequestDto(
        hash = hash(),
        from = from(),
        input = input(),
        nonce = nonce().toLong(),
        to = to()
    )

    @TestConfiguration
    class TestAssetTypeServiceConfiguration {
        @Bean
        @Primary
        fun mockedAssetTypeService(): AssetTypeService {
            return mockk()
        }
    }
}
