package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.crypto.punks.AssetMatcherPunk
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferProxy
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.data.isEqualToOrder
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.integration.TestPropertiesConfiguration
import com.rarible.protocol.order.listener.misc.sign
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class CryptoPunkRaribleOrdersTest : AbstractCryptoPunkTest() {

    private lateinit var exchangeV2: ExchangeV2
    private lateinit var eip712Domain: EIP712Domain
    private lateinit var punkTransferProxy: PunkTransferProxy
    private lateinit var assetMatcherPunk: AssetMatcherPunk
    private lateinit var wethContract: TestERC20
    private lateinit var erc20TransferProxy: ERC20TransferProxy

    @BeforeEach
    private fun initialize() = runBlocking {
        wethContract = TestERC20.deployAndWait(sender, poller, "WETH", "WETH").awaitSingle()
        erc20TransferProxy = ERC20TransferProxy.deployAndWait(sender, poller).awaitSingle()

        exchangeV2 = ExchangeV2.deployAndWait(sender, poller).awaitFirst()
        exchangeV2.__ExchangeV2_init(
            Address.ZERO(),
            erc20TransferProxy.address(),
            BigInteger.ZERO,
            Address.ZERO(),
            Address.ZERO()
        ).execute().verifySuccess()

        erc20TransferProxy.addOperator(exchangeV2.address()).execute().verifySuccess()

        eip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = exchangeV2.address()
        )
        prepareTxService.eip712Domain = eip712Domain
        exchangeContractAddresses.v2 = exchangeV2.address()

        punkTransferProxy = PunkTransferProxy.deployAndWait(sender, poller).awaitFirst()
        transferProxyAddresses.cryptoPunksTransferProxy = punkTransferProxy.address()
        exchangeV2.setTransferProxy(AssetType.CRYPTO_PUNKS.bytes(), punkTransferProxy.address())
            .execute().verifySuccess()

        assetMatcherPunk = AssetMatcherPunk.deployAndWait(sender, poller).awaitFirst()
        exchangeV2.setAssetMatcher(AssetType.CRYPTO_PUNKS.bytes(), assetMatcherPunk.address())
            .execute().verifySuccess()
        Unit
    }

    @Test
    fun `sell crypto punk via ExchangeV2`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender, sellerPrivateKey) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()
        val punkPrice = BigInteger.valueOf(100500)

        // Allow to sell the punk to the CryptoPunks transfer proxy for 0 ETH.
        cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, BigInteger.ZERO, punkTransferProxy.address())
            .withSender(sellerSender)
            .execute().verifySuccess()

        // Insert the Rarible sell order.
        val sellMake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val sellTake = Asset(EthAssetType, EthUInt256(punkPrice))

        val sellOrderVersion = OrderVersion(
            maker = sellerAddress,
            taker = null,
            make = sellMake,
            take = sellTake,
            type = OrderType.RARIBLE_V2,
            platform = Platform.CRYPTO_PUNKS,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            signature = null
        ).let {
            it.copy(
                signature = eip712Domain.hashToSign(Order.hash(it)).sign(sellerPrivateKey)
            )
        }

        val (buyerAddress, buyerSender) = newSender()
        depositInitialBalance(buyerAddress, punkPrice)
        val sellOrder = orderUpdateService.save(sellOrderVersion)
        val preparedBuyTx = prepareTxService.prepareTransaction(
            sellOrder,
            PrepareOrderTxFormDto(buyerAddress, BigInteger.ONE, emptyList(), emptyList())
        )
        buyerSender.sendTransaction(
            Transaction(
                exchangeV2.address(),
                buyerAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                punkPrice,
                preparedBuyTx.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            // Verify that the buyer has received the punk and the seller received the funds.
            assertEquals(buyerAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
            assertEquals(punkPrice, getEthBalance(sellerAddress))
        }

        Wait.waitAssert {
            val allOrders = orderRepository.findAll().toList()
            assertEquals(1, allOrders.size) {
                allOrders.joinToString(separator = "\n") { it.toString() }
            }
            assertEquals(1, allOrders.filter { it.type == OrderType.RARIBLE_V2 }.size)
            val sellOrderFilled = allOrders.single { it.type == OrderType.RARIBLE_V2 }
            assertEquals(EthUInt256(punkPrice), sellOrderFilled.fill)
            assertEquals(sellOrder.hash, sellOrderFilled.hash)
        }

        val buyOrderHash = Order.hashKey(buyerAddress, sellTake.type, sellMake.type, BigInteger.ZERO)
        Wait.waitAssert {
            val allHistories = exchangeHistoryRepository.findAll().collectList().awaitSingle()
                .map { it.data as OrderExchangeHistory }
            assertEquals(2, allHistories.size) {
                allHistories.joinToString(separator = "\n") { it.toString() }
            }

            val raribleItems = allHistories.filter { it.source == HistorySource.RARIBLE }
                .filterIsInstance<OrderSideMatch>()
            assertEquals(2, raribleItems.size)

            val sides = raribleItems.associateBy { it.side }

            val left = sides.getValue(OrderSide.LEFT)
            val right = sides.getValue(OrderSide.RIGHT)

            assertEquals(sellOrder.hash, left.hash)
            assertEquals(buyOrderHash, left.counterHash)
            assertEquals(sellerAddress, left.maker)
            assertEquals(buyerAddress, left.taker)
            assertEquals(sellMake, left.make)
            assertEquals(sellTake, left.take)

            assertEquals(buyOrderHash, right.hash)
            assertEquals(sellOrder.hash, right.counterHash)
            assertEquals(buyerAddress, right.maker)
            assertEquals(sellerAddress, right.taker)
            assertEquals(sellTake, right.make)
            assertEquals(sellMake, right.take)
        }

        checkActivityWasPublished {
            val listActivity = this
            assertTrue(this is OrderActivityListDto)
            listActivity as OrderActivityListDto
            assertEquals(sellOrder.hash, listActivity.hash)
            assertEquals(OrderActivityDto.Source.RARIBLE, listActivity.source)
        }

        checkActivityWasPublished {
            val matchActivity = this
            assertTrue(matchActivity is OrderActivityMatchDto)
            matchActivity as OrderActivityMatchDto
            assertEquals(OrderActivityDto.Source.RARIBLE, matchActivity.source)
            assertEquals(sellOrder.hash, matchActivity.left.hash)
            assertEquals(sellerAddress, matchActivity.left.maker)
            assertEquals(
                AssetDto(
                    CryptoPunksAssetTypeDto(
                        cryptoPunksMarket.address(),
                        punkIndex.toInt()
                    ), BigInteger.ONE, BigDecimal.ONE
                ), matchActivity.left.asset
            )
            assertEquals(buyOrderHash, matchActivity.right.hash)
            assertEquals(buyerAddress, matchActivity.right.maker)
            assertTrue(matchActivity.right.asset.assetType is EthAssetTypeDto)
            assertEquals(punkPrice, matchActivity.right.asset.value)
        }
    }

    /**
     * 1) Create Rarible sell order.
     * 2) Create CryptoPunksMarket order.
     * The approval to the Rarible transfer proxy is expired inside the CryptoPunksMarket,
     * so the Rarible order becomes INACTIVE.
     */
    @Test
    fun `on-chain sell order must make the Rarible sell order inactive`() = runBlocking<Unit> {
        val (sellerAddress, sellerSender, sellerPrivateKey) = newSender()
        val punkIndex = 42.toBigInteger()

        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()
        val rariblePunkPrice = BigInteger.valueOf(100000)

        // Allow to sell the punk to the CryptoPunks transfer proxy for 0 ETH.
        cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, BigInteger.ZERO, punkTransferProxy.address())
            .withSender(sellerSender)
            .execute().verifySuccess()

        // Insert the Rarible sell order.
        val make = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)
        val raribleTake = Asset(EthAssetType, EthUInt256(rariblePunkPrice))

        val sellOrderVersion = OrderVersion(
            maker = sellerAddress,
            taker = null,
            make = make,
            take = raribleTake,
            type = OrderType.RARIBLE_V2,
            platform = Platform.CRYPTO_PUNKS,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            signature = null
        ).let {
            it.copy(
                signature = eip712Domain.hashToSign(Order.hash(it)).sign(sellerPrivateKey)
            )
        }
        val sellOrder = orderUpdateService.save(sellOrderVersion)

        // Add on-chain sell order
        val onChainPunkPrice = BigInteger.valueOf(200000)
        val onChainSellOrderTimestamp = cryptoPunksMarket.offerPunkForSale(punkIndex, onChainPunkPrice)
            .withSender(sellerSender).execute().verifySuccess().getTimestamp()
        val onChainPunkPriceUsd = onChainPunkPrice.toBigDecimal(18) * TestPropertiesConfiguration.ETH_CURRENCY_RATE
        val onChainTake = Asset(EthAssetType, EthUInt256(onChainPunkPrice))
        val makePrice = onChainPunkPrice.toBigDecimal(18)

        Wait.waitAssert {
            val inactiveRaribleOrder = orderRepository.findById(sellOrder.hash)
            assertThat(inactiveRaribleOrder).isEqualToOrder(
                sellOrder.copy(
                    status = OrderStatus.INACTIVE,
                    makeStock = EthUInt256.ZERO,
                    dbUpdatedAt = inactiveRaribleOrder?.dbUpdatedAt
                )
            )
        }

        Wait.waitAssert {
            val activeOrders = orderRepository.findActive().toList()
            assertThat(activeOrders).hasSize(1)
            val onChainSellOrder = activeOrders.single()
            assertThat(onChainSellOrder).isEqualToOrder(
                Order(
                    maker = sellerAddress,
                    taker = null,
                    make = make,
                    take = onChainTake,
                    type = OrderType.CRYPTO_PUNKS,
                    fill = EthUInt256.ZERO,
                    cancelled = false,
                    makeStock = EthUInt256.ONE,
                    salt = CRYPTO_PUNKS_SALT,
                    start = null,
                    end = null,
                    data = OrderCryptoPunksData,
                    signature = null,
                    createdAt = onChainSellOrderTimestamp,
                    lastUpdateAt = onChainSellOrderTimestamp,
                    pending = emptyList(),
                    makePriceUsd = onChainPunkPriceUsd,
                    takePriceUsd = null,
                    makePrice = makePrice,
                    takePrice = null,
                    makeUsd = null,
                    takeUsd = onChainPunkPriceUsd,
                    priceHistory = createPriceHistory(onChainSellOrderTimestamp, make, onChainTake),
                    status = OrderStatus.ACTIVE,
                    platform = Platform.CRYPTO_PUNKS,
                    lastEventId = onChainSellOrder.lastEventId,
                    dbUpdatedAt = onChainSellOrder.dbUpdatedAt
                )
            )
        }
    }

    @Test
    fun `buy crypto punk via ExchangeV2`() = runBlocking<Unit> {
        val (ownerAddress, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        // Create a bidder willing to pay in WETH.
        val bidPrice = BigInteger.valueOf(100500)
        val (bidderAddress, bidderSender, bidderPrivateKey) = newSender()
        wethContract.mint(bidderAddress, bidPrice).execute().verifySuccess()
        wethContract.approve(erc20TransferProxy.address(), bidPrice).withSender(bidderSender).execute().verifySuccess()

        val bidMake = Asset(Erc20AssetType(wethContract.address()), EthUInt256(bidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), EthUInt256(punkIndex)), EthUInt256.ONE)

        // Insert the Rarible bid order.
        val bidOrderVersion = OrderVersion(
            maker = bidderAddress,
            taker = ownerAddress,
            make = bidMake,
            take = bidTake,
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null,
            signature = null
        ).let {
            it.copy(
                signature = eip712Domain.hashToSign(Order.hash(it)).sign(bidderPrivateKey)
            )
        }
        val bidOrder = orderUpdateService.save(bidOrderVersion)

        // Allow to sell the punk to the CryptoPunks transfer proxy for 0 ETH.
        cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, BigInteger.ZERO, punkTransferProxy.address())
            .withSender(ownerSender)
            .execute().verifySuccess()

        // Prepare and send the sell transaction from the punk owner.
        val preparedAcceptBidTx = prepareTxService.prepareTransaction(
            bidOrder,
            PrepareOrderTxFormDto(ownerAddress, BigInteger.ONE, emptyList(), emptyList())
        )
        ownerSender.sendTransaction(
            Transaction(
                exchangeV2.address(),
                ownerAddress,
                500000.toBigInteger(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                preparedAcceptBidTx.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            // Verify that the bidder has received the punk and the owner received the funds.
            assertEquals(bidderAddress, cryptoPunksMarket.punkIndexToAddress(punkIndex).awaitSingle())
            assertEquals(bidPrice, wethContract.balanceOf(ownerAddress).call().awaitSingle())
        }

        Wait.waitAssert {
            val allOrders = orderRepository.findAll().toList()
            assertEquals(1, allOrders.size) {
                allOrders.joinToString(separator = "\n") { it.toString() }
            }
            assertEquals(1, allOrders.filter { it.type == OrderType.RARIBLE_V2 }.size)
            val bidOrderFilled = allOrders.single { it.type == OrderType.RARIBLE_V2 }
            assertEquals(EthUInt256.ONE, bidOrderFilled.fill)
            assertEquals(bidOrder.hash, bidOrderFilled.hash)
        }

        Wait.waitAssert {
            val allHistories = exchangeHistoryRepository.findAll().collectList().awaitSingle()
                .map { it.data as OrderExchangeHistory }
            assertEquals(2, allHistories.size) {
                allHistories.joinToString(separator = "\n") { it.toString() }
            }

            val raribleItems =
                allHistories.filter { it.source == HistorySource.RARIBLE }.filterIsInstance<OrderSideMatch>()
            assertEquals(2, raribleItems.size)

            val sides = raribleItems.associateBy { it.side }

            val left = sides.getValue(OrderSide.LEFT)
            val right = sides.getValue(OrderSide.RIGHT)

            assertEquals(bidderAddress, left.maker)
            assertEquals(ownerAddress, left.taker)
            assertEquals(bidMake, left.make)
            assertEquals(bidTake, left.take)
            assertEquals(bidTake.value, left.fill)

            assertEquals(ownerAddress, right.maker)
            assertEquals(bidderAddress, right.taker)
            assertEquals(bidTake, right.make)
            assertEquals(bidMake, right.take)
            assertEquals(bidMake.value, right.fill)
        }

        checkActivityWasPublished {
            val bidActivity = this
            assertTrue(bidActivity is OrderActivityBidDto)
            bidActivity as OrderActivityBidDto
            assertEquals(bidOrder.hash, bidActivity.hash)
        }

        checkActivityWasPublished {
            val matchActivity = this
            assertTrue(matchActivity is OrderActivityMatchDto)
            matchActivity as OrderActivityMatchDto
            assertEquals(OrderActivityDto.Source.RARIBLE, matchActivity.source)
            assertEquals(bidOrder.hash, matchActivity.left.hash)
            assertEquals(bidderAddress, matchActivity.left.maker)
            assertTrue(matchActivity.left.asset.assetType is Erc20AssetTypeDto)

            assertEquals(
                AssetDto(
                    CryptoPunksAssetTypeDto(
                        cryptoPunksMarket.address(),
                        punkIndex.toInt()
                    ), BigInteger.ONE, BigDecimal.ONE
                ), matchActivity.right.asset
            )
            assertEquals(ownerAddress, matchActivity.right.maker)
        }
    }
}
