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
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.setField
import com.rarible.protocol.order.listener.misc.sign
import com.rarible.protocol.order.listener.service.descriptors.exchange.v2.ExchangeOrderMatchDescriptor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger

@IntegrationTest
@FlowPreview
class CryptoPunkRaribleOrdersTest : AbstractCryptoPunkTest() {

    @Autowired
    private lateinit var exchangeOrderMatchDescriptor: ExchangeOrderMatchDescriptor

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
        setField(prepareTxService, "eip712Domain", eip712Domain)
        setField(exchangeOrderMatchDescriptor, "exchangeContract", exchangeV2.address())

        punkTransferProxy = PunkTransferProxy.deployAndWait(sender, poller).awaitFirst()
        exchangeV2.setTransferProxy(AssetType.CRYPTO_PUNKS.bytes(), punkTransferProxy.address())
            .execute().verifySuccess()

        assetMatcherPunk = AssetMatcherPunk.deployAndWait(sender, poller).awaitFirst()
        exchangeV2.setAssetMatcher(AssetType.CRYPTO_PUNKS.bytes(), assetMatcherPunk.address())
            .execute().verifySuccess()
        Unit
    }

    @Test
    fun `sell crypto punk via ExchangeV2`() = runBlocking {
        val (sellerAddress, sellerSender, sellerPrivateKey) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(sellerSender).execute().verifySuccess()
        val punkPrice = BigInteger.valueOf(100500)

        // Allow to sell the punk to the CryptoPunks transfer proxy for 0 ETH.
        cryptoPunksMarket.offerPunkForSaleToAddress(punkIndex, BigInteger.ZERO, punkTransferProxy.address())
            .withSender(sellerSender)
            .execute().verifySuccess()

        // Insert the Rarible sell order.
        val sellMake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), punkIndex.toInt()), EthUInt256.ONE)
        val sellTake = Asset(EthAssetType, EthUInt256(punkPrice))

        val sellOrderVersion = OrderVersion(
            maker = sellerAddress,
            taker = null,
            make = sellMake,
            take = sellTake,
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
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
            assertEquals(5, allOrders.size) {
                allOrders.joinToString(separator = "\n") { it.toString() }
            }

            // There are 4 on-chain orders for CryptoPunks:
            // 2 for sale/buy between the punk owner and proxy
            // 2 for sale/buy between the proxy and the new owner
            assertEquals(4, allOrders.filter { it.type == OrderType.CRYPTO_PUNKS }.size)

            assertEquals(1, allOrders.filter { it.type == OrderType.RARIBLE_V2 }.size)
            val sellOrderFilled = allOrders.single { it.type == OrderType.RARIBLE_V2 }
            assertEquals(EthUInt256(punkPrice), sellOrderFilled.fill)
        }

        /*
            We must observe the following 10 histories:
            - 1 SELL ON-CHAIN-ORDER for "allow buying punk for 0ETH SELLER -> PROXY"
            - 3 ON-CHAIN-ORDERS for "buyPunk" SELLER -> PROXY
              - 1 BUY ORDER 0ETH SELLER -> PROXY
              - 1 SIDE MATCH 0ETH SELLER -> PROXY
              - 1 SIDE MATCH 0ETH PROXY -> SELLER
            - 4 ON-CHAIN-ORDERS for "transferPunk" from PROXY to BUYER
              - 1 SELL ORDER 0ETH PROXY -> BUYER
              - 1 BUY ORDER 0ETH BUYER -> PROXY
              - 1 SIDE MATCH 0ETH PROXY -> BUYER
              - 1 SIDE MATCH 0ETH BUYER -> PROXY
            - 2 RARIBLE ORDERS
              - 1 SIDE MATCH 1PUNK SELLER -> BUYER
              - 1 SIDE MATCH 99ETH BUYER -> SELLER
         */
        Wait.waitAssert {
            val allHistories = exchangeHistoryRepository.findAll().collectList().awaitSingle()
                .map { it.data as OrderExchangeHistory }
            assertEquals(10, allHistories.size) {
                allHistories.joinToString(separator = "\n") { it.toString() }
            }

            val raribleItems =
                allHistories.filter { it.source == HistorySource.RARIBLE }.filterIsInstance<OrderSideMatch>()
            assertEquals(2, raribleItems.size)
            assertEquals(8, allHistories.filter { it.source == HistorySource.CRYPTO_PUNKS }.size)

            val sides = raribleItems.associateBy { it.side }

            val left = sides.getValue(OrderSide.LEFT)
            val right = sides.getValue(OrderSide.RIGHT)

            assertEquals(sellerAddress, left.maker)
            assertEquals(buyerAddress, left.taker)
            assertEquals(sellMake, left.make)
            assertEquals(sellTake, left.take)

            assertEquals(buyerAddress, right.maker)
            assertEquals(sellerAddress, right.taker)
            assertEquals(sellTake, right.make)
            assertEquals(sellMake, right.take)
        }

        /*
            We must observe the following 6 published activities:
            - 2 Rarible activities
              - 1 Rarible List from SELLER
              - 1 Rarible Match for SELLER <-> BUYER
            - 4 on-chain activities
              - 1 Bid + 1 Match for SELLER <-> PROXY
              - 1 Bid + 1 Match for PROXY <-> BUYER
         */
        checkPublishedActivities { activities ->
            val leftAsset = AssetDto(
                CryptoPunksAssetTypeDto(cryptoPunksMarket.address(), punkIndex.toInt()), BigInteger.ONE
            )
            val rightHash = Order.hashKey(
                buyerAddress,
                EthAssetType,
                CryptoPunksAssetType(cryptoPunksMarket.address(), punkIndex.toInt()),
                CRYPTO_PUNKS_SALT.value
            )
            assertTrue(
                activities.any {
                    it is OrderActivityMatchDto
                            && it.source == OrderActivityDto.Source.RARIBLE
                            && it.left.hash == sellOrder.hash
                            && it.left.maker == sellerAddress
                            && it.left.asset == leftAsset

                            && it.right.hash == rightHash
                            && it.right.maker == buyerAddress
                            && it.right.asset.assetType is EthAssetTypeDto
                            && it.right.asset.value == punkPrice
                }
            )

            assertEquals(6, activities.size) {
                activities.joinToString(separator = "\n") { it.toString() }
            }
            assertEquals(1, activities.count {
                it is OrderActivityListDto && it.source == OrderActivityDto.Source.RARIBLE
            })
            assertEquals(1, activities.count {
                it is OrderActivityMatchDto && it.source == OrderActivityDto.Source.RARIBLE
            })
            assertEquals(2, activities.count {
                it is OrderActivityBidDto && it.source == OrderActivityDto.Source.CRYPTO_PUNKS
            })
            assertEquals(2, activities.count {
                it is OrderActivityMatchDto && it.source == OrderActivityDto.Source.CRYPTO_PUNKS
            })
        }
    }

    @Test
    fun `buy crypto punk via ExchangeV2`() = runBlocking {
        val (ownerAddress, ownerSender) = newSender()
        val punkIndex = 42.toBigInteger()
        cryptoPunksMarket.getPunk(punkIndex).withSender(ownerSender).execute().verifySuccess()

        // Create a bidder willing to pay in WETH.
        val bidPrice = BigInteger.valueOf(100500)
        val (bidderAddress, bidderSender, bidderPrivateKey) = newSender()
        wethContract.mint(bidderAddress, bidPrice).execute().verifySuccess()
        wethContract.approve(erc20TransferProxy.address(), bidPrice).withSender(bidderSender).execute().verifySuccess()

        val bidMake = Asset(Erc20AssetType(wethContract.address()), EthUInt256(bidPrice))
        val bidTake = Asset(CryptoPunksAssetType(cryptoPunksMarket.address(), punkIndex.toInt()), EthUInt256.ONE)

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
            assertEquals(5, allOrders.size) {
                allOrders.joinToString(separator = "\n") { it.toString() }
            }

            // There are 4 on-chain orders for CryptoPunks:
            // 2 for sale/buy between the punk owner and proxy
            // 2 for sale/buy between the proxy and the new owner
            assertEquals(4, allOrders.filter { it.type == OrderType.CRYPTO_PUNKS }.size)

            assertEquals(1, allOrders.filter { it.type == OrderType.RARIBLE_V2 }.size)
            val bidOrderFilled = allOrders.single { it.type == OrderType.RARIBLE_V2 }
            assertEquals(EthUInt256.ONE, bidOrderFilled.fill)
        }

        /*
            We must observe the following 10 histories:
            - 1 SELL ON-CHAIN-ORDER for "allow buying punk for 0ETH SELLER -> PROXY"
            - 3 ON-CHAIN-ORDERS for "buyPunk" SELLER -> PROXY
              - 1 BUY ORDER 0ETH SELLER -> PROXY
              - 1 SIDE MATCH 0ETH SELLER -> PROXY
              - 1 SIDE MATCH 0ETH PROXY -> SELLER
            - 4 ON-CHAIN-ORDERS for "transferPunk" from PROXY to BUYER
              - 1 SELL ORDER 0ETH PROXY -> BUYER
              - 1 BUY ORDER 0ETH BUYER -> PROXY
              - 1 SIDE MATCH 0ETH PROXY -> BUYER
              - 1 SIDE MATCH 0ETH BUYER -> PROXY
            - 2 RARIBLE ORDERS
              - 1 SIDE MATCH 1PUNK SELLER -> BUYER
              - 1 SIDE MATCH 100500 WETH BUYER -> SELLER
         */
        Wait.waitAssert {
            val allHistories = exchangeHistoryRepository.findAll().collectList().awaitSingle()
                .map { it.data as OrderExchangeHistory }
            assertEquals(10, allHistories.size) {
                allHistories.joinToString(separator = "\n") { it.toString() }
            }

            val raribleItems =
                allHistories.filter { it.source == HistorySource.RARIBLE }.filterIsInstance<OrderSideMatch>()
            assertEquals(2, raribleItems.size)
            assertEquals(8, allHistories.filter { it.source == HistorySource.CRYPTO_PUNKS }.size)

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

        /*
            We must observe the following 6 published activities:
            - 2 Rarible activities
              - 1 Rarible Bid from BUYER
              - 1 Rarible Match for BUYER <-> SELLER
            - 4 on-chain activities
              - 1 Bid + 1 Match for SELLER <-> PROXY
              - 1 Bid + 1 Match for PROXY <-> BUYER
         */
        checkPublishedActivities { activities ->
            val assetDto = AssetDto(
                CryptoPunksAssetTypeDto(
                    cryptoPunksMarket.address(),
                    punkIndex.toInt()
                ), BigInteger.ONE
            )
            assertTrue(activities.any {
                it is OrderActivityMatchDto
                        && it.source == OrderActivityDto.Source.RARIBLE
                        && it.left.hash == bidOrder.hash
                        && it.left.maker == bidderAddress
                        && it.left.asset.assetType is Erc20AssetTypeDto

                        && it.right.asset == assetDto
                        && it.right.maker == ownerAddress
            })

            assertEquals(6, activities.size) {
                activities.joinToString(separator = "\n") { it.toString() }
            }
            assertEquals(1, activities.count {
                it is OrderActivityBidDto && it.source == OrderActivityDto.Source.RARIBLE
            })
            assertEquals(1, activities.count {
                it is OrderActivityMatchDto && it.source == OrderActivityDto.Source.RARIBLE
            })
            assertEquals(2, activities.count {
                it is OrderActivityBidDto && it.source == OrderActivityDto.Source.CRYPTO_PUNKS
            })
            assertEquals(2, activities.count {
                it is OrderActivityMatchDto && it.source == OrderActivityDto.Source.CRYPTO_PUNKS
            })
        }
    }
}