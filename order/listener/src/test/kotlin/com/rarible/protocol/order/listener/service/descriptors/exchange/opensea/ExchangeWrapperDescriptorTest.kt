package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.common.opensea.merkle.MerkleValidator
import com.rarible.protocol.contracts.common.wyvern.proxy.WyvernTokenTransferProxy
import com.rarible.protocol.contracts.common.wyvern.registry.WyvernProxyRegistry
import com.rarible.protocol.contracts.common.wyvern.token.TestToken
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.contracts.exchange.wrapper.ExchangeWrapper
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.sign
import com.rarible.protocol.order.listener.service.descriptors.exchange.opensea.AbstractOpenSeaV1Test.Companion.WORD_ZERO
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoSigningTransactionSender
import java.math.BigInteger

@IntegrationTest
class ExchangeWrapperDescriptorTest : AbstractIntegrationTest() {

    protected val privateKey1: BigInteger =
        "63549542177354512953226189656753882128250349867832707824448011056502229857445".toBigInteger()
    protected lateinit var userSender1: MonoSigningTransactionSender

    protected val privateKey2: BigInteger =
        "7968570106556392458827029065766075642835086546292380755068224199094000053141".toBigInteger()
    protected lateinit var userSender2: MonoSigningTransactionSender
    protected lateinit var openseaFeeAddress: Address

    protected lateinit var testToken: TestToken
    protected lateinit var wyvernProxyRegistry: WyvernProxyRegistry
    protected lateinit var wyvernTokenTransferProxy: WyvernTokenTransferProxy
    protected lateinit var exchangeOpenSea: WyvernExchange
    protected lateinit var exchangeOpenSeaV2: WyvernExchange
    protected lateinit var exchangeWrapper: ExchangeWrapper
    protected lateinit var merkleValidator: MerkleValidator
    protected lateinit var user1RegisteredProxy: Address

    protected lateinit var token721: TestERC721

    protected lateinit var wrapperHelper: WrapperHelper

    protected lateinit var transferProxy: TransferProxy
    protected lateinit var erc20TransferProxy: ERC20TransferProxy

    @Autowired
    private lateinit var prepareTxService: PrepareTxService

    @Autowired
    private lateinit var callDataEncoder: CallDataEncoder

    @Autowired
    private lateinit var commonSigner: CommonSigner

    @BeforeEach
    fun before() = runBlocking {
        userSender1 = newSender(privateKey1).second
        userSender2 = newSender(privateKey2).second
        openseaFeeAddress = newSender().second.from()

        wyvernProxyRegistry = WyvernProxyRegistry.deployAndWait(sender, poller).awaitFirst()
        wyvernTokenTransferProxy =
            WyvernTokenTransferProxy.deployAndWait(sender, poller, wyvernProxyRegistry.address()).awaitFirst()
        testToken = TestToken.deployAndWait(sender, poller).awaitFirst()

        token721 = TestERC721.deployAndWait(sender, poller, "Test", "TST").awaitFirst()

        exchangeOpenSea = WyvernExchange.deployAndWait(
            sender,
            poller,
            wyvernProxyRegistry.address(),
            wyvernTokenTransferProxy.address(),
            testToken.address(),
            openseaFeeAddress
        ).awaitFirst()

        exchangeOpenSeaV2 = WyvernExchange.deployAndWait(
            sender,
            poller,
            wyvernProxyRegistry.address(),
            wyvernTokenTransferProxy.address(),
            testToken.address(),
            openseaFeeAddress
        ).awaitFirst()

        val exchangeRaribleV2 = ExchangeV2.deployAndWait(sender, poller).awaitFirst()

        exchangeWrapper = ExchangeWrapper.deployAndWait(
            sender,
            poller
        ).awaitFirst()

        exchangeWrapper.__ExchangeWrapper_init(
            exchangeOpenSea.address(),
            exchangeRaribleV2.address()
        ).execute().verifySuccess()

        merkleValidator = MerkleValidator.deployAndWait(
            sender,
            poller
        ).awaitFirst()

        exchangeContractAddresses.openSeaV1 = exchangeOpenSea.address()
        exchangeContractAddresses.openSeaV2 = exchangeOpenSeaV2.address()
        exchangeContractAddresses.exchangeWrapper = exchangeWrapper.address()

        wyvernProxyRegistry.grantInitialAuthentication(exchangeOpenSea.address()).execute().verifySuccess()
        wyvernProxyRegistry.registerProxy().withSender(userSender1).execute().awaitFirst()
        user1RegisteredProxy = wyvernProxyRegistry.proxies(userSender1.from()).awaitFirst()

        token721.setApprovalForAll(user1RegisteredProxy, true).withSender(userSender1).execute().verifySuccess()
        transferProxy = TransferProxy.deployAndWait(sender, poller).block()!!
        erc20TransferProxy = ERC20TransferProxy.deployAndWait(sender, poller).block()!!

        // ====================

        logInit()

        wrapperHelper = WrapperHelper(
            prepareTxService = prepareTxService,
            exchangeContractAddresses = exchangeContractAddresses
        )
    }

    @Disabled
    @Test
    fun `should execute sell order with call method`() = runBlocking {
        val sellMaker = userSender1.from()
        val buyMaker = userSender2.from()

        val token = token721.address()
        val tokenId = EthUInt256.ONE
        token721.mint(sellMaker, tokenId.value, "test").execute().verifySuccess()

        val nftPrice = BigInteger.valueOf(100_000_000_000_000)
        val sellMake = Asset(Erc721AssetType(token, tokenId), EthUInt256.ONE)
        val sellTake = Asset(EthAssetType, EthUInt256.of(nftPrice))

        val balance = getEthBalance(userSender2.from())
        val newBalance = nftPrice.multiply(BigInteger.valueOf(300))
        depositInitialBalance(userSender1.from(), newBalance)
        depositInitialBalance(userSender2.from(), newBalance)
        val balance2 = getEthBalance(userSender2.from())

        val sellCallData = callDataEncoder.encodeTransferCallData(
            Transfer.MerkleValidatorErc721Transfer(
                from = sellMaker,
                to = Address.ZERO(),
                token = token,
                tokenId = tokenId.value,
                root = WORD_ZERO,
                proof = emptyList(),
                safe = false
            )
        )

        val sellOrderVersion = OrderVersion(
            maker = sellMaker,
            taker = null,
            make = sellMake,
            take = sellTake,
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.of("23741570621504096017579347520175475668997079901727111482740544775140580203952"),
            start = nowMillis().epochSecond - 10,
            end = null,
            signature = null,
            data = OrderOpenSeaV1DataV1(
                exchange = exchangeOpenSea.address(),
                makerRelayerFee = BigInteger.valueOf(250),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = exchangeOpenSea.protocolFeeRecipient().awaitFirst(),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = sellCallData.callData,
                replacementPattern = sellCallData.replacementPattern,
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = merkleValidator.address(),
                nonce = null,
            ),
            platform = Platform.OPEN_SEA,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            val hash = Order.hash(it) // Recalculate OpenSea's specific order hash
            val hashToSign = commonSigner.ethSignHashToSign(hash)
            logger.info("Sell order hash: $hash, hash to sing: $hashToSign")
            val signature = hashToSign.sign(privateKey1)
            it.copy(signature = signature, hash = hash)
        }

        val sellOrder = orderUpdateService.save(sellOrderVersion)

        // =====  Buy Side =====

        val callData =
            wrapperHelper.prepareTxForWrappedExchange(
                order = sellOrder,
                form = PrepareOrderTxFormDto(
                    maker = buyMaker,
                    amount = BigInteger.ONE,
                    originFees = emptyList(),
                    payouts = emptyList()
                ),
                price = nftPrice
            ).transaction.data

        printAddresses()
        CallDataPrinter().printSinglePurchase(callData)

        userSender2.sendTransaction(
            Transaction(
                /* to = */ exchangeWrapper.address(),
                /* from = */ userSender2.from(),
                /* gas = */ 8_000_000.toBigInteger(),
                /* gasPrice = */ BigInteger.ZERO,
                /* value = */ nftPrice,
//                /* value = */ nftPrice.multiply(BigInteger.valueOf(2)),
                /* data = */ callData,
                /* nonce = */ null
            )
        ).verifySuccess()

        // ====== Assert =======

        assert(sellOrder)
    }

    private suspend fun printAddresses() {
        println("ExchangeOpenSea: ${exchangeOpenSea.address()}")
        println("ExchangeWrapper: ${exchangeWrapper.address()}")
        println("MerkleValidator: ${merkleValidator.address()}")
        println("Token721:        ${token721.address()}")
        println("openseaFeeAddress ${openseaFeeAddress}")
        println("User1:           ${userSender1.from()}")
        println("User2:           ${userSender2.from()}")
        println("User1 balance:   ${getEthBalance(userSender1.from())}")
        println("User2 balance:   ${getEthBalance(userSender2.from())}")
    }

    private suspend fun assert(sellOrder: Order) {
        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            Assertions.assertThat(items).hasSize(2)

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            Assertions.assertThat(left?.fill).isEqualTo(EthUInt256.TEN)
            Assertions.assertThat(right?.fill).isEqualTo(EthUInt256.ONE)

            Assertions.assertThat(left?.make)
                .isEqualTo(sellOrder.make)
            Assertions.assertThat(left?.take)
                .isEqualTo(sellOrder.take)
            Assertions.assertThat(left?.externalOrderExecutedOnRarible).isTrue()

            Assertions.assertThat(right?.make)
                .isEqualTo(sellOrder.take)
            Assertions.assertThat(right?.take)
                .isEqualTo(sellOrder.make)
            Assertions.assertThat(right?.externalOrderExecutedOnRarible).isTrue()

            val filledOrder = orderRepository.findById(sellOrder.hash)
            Assertions.assertThat(filledOrder?.fill).isEqualTo(EthUInt256.TEN)

            assertFalse(left?.adhoc!!)
            assertTrue(left.counterAdhoc!!)

            assertTrue(right?.adhoc!!)
            assertFalse(right.counterAdhoc!!)

            checkActivityWasPublished {
                Assertions.assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                    Assertions.assertThat(left.hash).isEqualTo(sellOrder.hash)
                }
            }
        }
    }

    private fun logInit() {

        logger.info("User1: ${userSender1.from()}")
        logger.info("User2: ${userSender2.from()}")

        logger.info("ProxyRegistryOpenSea: ${wyvernProxyRegistry.address()}")
        logger.info("proxyTokenTransferOpenSea: ${wyvernTokenTransferProxy.address()}")
        logger.info("user1RegisteredProxy: ${user1RegisteredProxy}")
        logger.info("TestToken: ${testToken.address()}")

        logger.info("TransferProxy: ${transferProxy.address()}")
        logger.info("Erc20TransferProxy: ${erc20TransferProxy.address()}")
        logger.info("Token721: ${token721.address()}")

        logger.info("ExchangeOpenSea: ${exchangeOpenSea.address()}")
        logger.info("ExchangeWrapper: ${exchangeWrapper.address()}")
    }
}
