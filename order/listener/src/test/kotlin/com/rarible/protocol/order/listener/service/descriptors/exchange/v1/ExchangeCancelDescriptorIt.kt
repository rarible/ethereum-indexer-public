package com.rarible.protocol.order.listener.service.descriptors.exchange.v1

import com.rarible.blockchain.scanner.block.Block
import com.rarible.blockchain.scanner.block.BlockStatus
import com.rarible.blockchain.scanner.ethereum.EthereumScannerManager
import com.rarible.blockchain.scanner.ethereum.client.EthereumClient
import com.rarible.blockchain.scanner.handler.TypedBlockRange
import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.common.deprecated.TransferProxyForDeprecated
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v1.state.ExchangeStateV1
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AutoReduce
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.LegacyAssetTypeClass.ERC1155
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.AutoReduceRepository
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scala.Tuple3
import scala.Tuple4
import scalether.domain.Address
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@IntegrationTest
class ExchangeCancelDescriptorIt : AbstractIntegrationTest() {
    companion object {
        lateinit var userSender: MonoSigningTransactionSender
        lateinit var token: TestERC1155
        lateinit var buyToken: TestERC1155

        private val salt = BigInteger.TEN
        private val tokenId = BigInteger.ONE
        private val buyTokenId = BigInteger.TEN
    }

    @Autowired
    private lateinit var manager: EthereumScannerManager

    @Autowired
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Autowired
    private lateinit var ethereumClient: EthereumClient

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
        token = TestERC1155.deployAndWait(userSender, poller).block()!!
        buyToken = TestERC1155.deployAndWait(userSender, poller).block()!!
    }

    @Test
    fun convert() = runBlocking<Unit> {
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
        val sale = ExchangeV1.deployAndWait(
            userSender,
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

        val orderLeftVersion = OrderVersion(
            maker = sender.from(),
            taker = null,
            make = Asset(Erc1155AssetType(token.address(), EthUInt256.of(tokenId)), EthUInt256.TEN),
            take = Asset(Erc1155AssetType(buyToken.address(), EthUInt256.of(buyTokenId)), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.of(salt),
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        )
        save(orderLeftVersion)

        val orderKey = Tuple4(
            sender.from(),
            salt,
            Tuple3(token.address(), tokenId, ERC1155.value),
            Tuple3(buyToken.address(), buyTokenId, ERC1155.value)
        )

        val saleReceipt = sale.cancel(orderKey)
            .withSender(sender)
            .execute().verifySuccess()

        exchangeContractAddresses.v1 = sale.address()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.CANCEL).collectList().awaitFirst()
            assertThat(items).hasSize(1)

            val cancel = items.single().data as OrderCancel

            val order = orderRepository.findById(orderLeftVersion.hash)
            assertThat(order?.cancelled).isEqualTo(true)

            val failMessage = "result: $cancel"

// todo            assertThat(cancel.owner).isEqualTo(sender.from()).withFailMessage(failMessage)
//            assertThat(cancel.makeToken).isEqualTo(token.address()).withFailMessage(failMessage)
//            assertThat(cancel.makeTokenId).isEqualTo(tokenId.toString()).withFailMessage(failMessage)
//
//            assertThat(cancel.takeToken).isEqualTo(buyToken.address()).withFailMessage(failMessage)
//            assertThat(cancel.takeTokenId).isEqualTo(buyTokenId.toString()).withFailMessage(failMessage)
//
//            assertThat(cancel.salt).isEqualTo(salt).withFailMessage(failMessage)

            val completed = state.getCompleted(orderKey).awaitFirst()
            assertThat(completed?.toString()).isEqualTo("115792089237316195423570985008687907853269984665640564039457584007913129639935")

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityCancelBidDto::class.java) {
                    assertThat(it.hash).isEqualTo(orderLeftVersion.hash)
                }
            }
        }

        val block = ethereumClient.getBlock(saleReceipt.blockNumber().longValueExact() - 1)

        manager.blockReindexer.reindex(
            baseBlock = Block(
                id = block.number,
                hash = block.hash,
                parentHash = block.parentHash,
                timestamp = block.timestamp,
                status = BlockStatus.SUCCESS,
                errors = emptyList()
            ),
            blocksRanges = flowOf(
                TypedBlockRange(
                    range = LongRange(
                        start = saleReceipt.blockNumber().longValueExact(),
                        endInclusive = saleReceipt.blockNumber().longValueExact()
                    ),
                    stable = true,
                )
            )
        ).collect()
        Wait.waitAssert {
            assertThat(autoReduceRepository.findOrders().toList()).containsExactly(
                AutoReduce(orderLeftVersion.hash.toString())
            )
        }
    }
}
