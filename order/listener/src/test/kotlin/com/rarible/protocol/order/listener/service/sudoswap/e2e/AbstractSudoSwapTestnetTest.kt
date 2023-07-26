package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.dto.AmmOrderDto
import com.rarible.protocol.order.api.ApiClient
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.SudoSwapBuyInfo
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Response
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import io.netty.channel.ChannelException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.time.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import scala.reflect.Manifest
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Paths
import java.time.Duration
import java.util.Properties
import kotlin.io.path.inputStream

abstract class AbstractSudoSwapTestnetTest {
    private val properties = Properties().apply {
        load(Paths.get("src/test/resources/local.properties").inputStream())
    }
    private val ethereumUri = properties["TESTNET_HOST"].toString()
    private val privateKey = Binary.apply(properties["PRIVATE_KEY"].toString())
    protected val sudoswapPairFactory: Address = Address.apply(Binary.apply(properties["SUDOSWAP_PAIR_FACTORY"].toString()))
    protected val sudoswapExponentialCurve: Address = Address.apply(Binary.apply(properties["SUDOSWAP_EXPONENTIAL_CURVE"].toString()))
    protected val sudoswapLinerCurve: Address = Address.apply(Binary.apply(properties["SUDOSWAP_LINER_CURVE"].toString()))
    protected val sudoSwapEventConverter = SudoSwapEventConverter(mockk())

    protected val ethereum = createEthereum(ethereumUri)
    protected val poller = MonoTransactionPoller(ethereum)

    // Use port forwarding to specify the host like http://localhost:47365
    private val ethereumOrderApi = createEthereumOrderApi(properties["ETHEREUM_API_HOST"].toString())

    protected val userSender = MonoSigningTransactionSender(
        ethereum,
        MonoSimpleNonceProvider(ethereum),
        privateKey.toBigInteger(),
        BigInteger.valueOf(8000000)
    ) { Mono.just(BigInteger.valueOf(800000)) }

    private fun createEthereum(ethereumUri: String): MonoEthereum {
        val requestTimeoutMs = 10000
        val readWriteTimeoutMs = 10000
        val maxFrameSize = 1024 * 1024
        val retryMaxAttempts = 5L
        val retryBackoffDelay = 100L

        val retry = Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryBackoffDelay))
            .filter { it is WebClientException || it is IOException || it is ChannelException }
        val transport = object : WebClientTransport(
            ethereumUri,
            MonoEthereum.mapper(),
            requestTimeoutMs,
            readWriteTimeoutMs
        ) {
            override fun maxInMemorySize(): Int = maxFrameSize
            override fun <T : Any?> get(url: String?, manifest: Manifest<T>?): Mono<T> =
                super.get(url, manifest).retryWhen(retry)
            override fun <T : Any?> send(request: Request?, manifest: Manifest<T>?): Mono<Response<T>> {
                return super.send(request, manifest).retryWhen(retry)
            }
        }
        return MonoEthereum(transport)
    }

    protected suspend fun createToken(
        sender: MonoSigningTransactionSender,
        poller: MonoTransactionPoller
    ): TestERC721 {
        return TestERC721.deployAndWait(sender, poller, "ipfs:/", "test").awaitFirst()
    }

    protected suspend fun mint(
        sender: MonoSigningTransactionSender,
        token: TestERC721,
        tokenId: BigInteger = randomBigInt()
    ): BigInteger {
        token
            .mint(sender.from(), tokenId)
            .execute()
            .verifySuccess()

        return tokenId
    }

    private fun getPoolAddressFromCreateLog(receipt: TransactionReceipt): Address {
        val logs = receipt.logs()
        val event = logs.find { it.topics().head() == NewPairEvent.id() }.get()
        return NewPairEvent.apply(event).poolAddress()
    }

    protected suspend fun mintAndApprove(
        mintCount: Int,
        sender: MonoSigningTransactionSender,
        token: TestERC721,
        approveTo: Address
    ): List<BigInteger> {
        return (1..mintCount).map {
            val tokenId = mint(sender, token)
            token.approve(approveTo, tokenId).execute().verifySuccess()
            tokenId
        }
    }

    private suspend fun getAmmOrder(hash: Word): AmmOrderDto {
        val amm = Wait.waitFor(Duration.ofSeconds(20)) {
            try {
                val order = ethereumOrderApi.getOrderByHash(hash.prefixed()).awaitFirstOrNull()
                assertThat(order).isNotNull
                assertThat(order).isInstanceOf(AmmOrderDto::class.java)
                order as AmmOrderDto
            } catch (ex: Exception) {
                null
            }
        }
        return requireNotNull(amm)
    }

    protected suspend fun checkOrder(hash: Word, callable: suspend (AmmOrderDto) -> Unit) {
        Wait.waitAssert(Duration.ofSeconds(20)) {
            val amm = getAmmOrder(hash)
            callable(amm)
        }
    }

    protected suspend fun createPool(
        sender: MonoSigningTransactionSender,
        nft: Address,
        bondingCurve: Address = sudoswapLinerCurve,
        assetRecipient: Address = randomAddress(),
        poolType: SudoSwapPoolType = SudoSwapPoolType.NFT,
        delta: BigInteger = BigDecimal.valueOf(0.2).multiply(decimal).toBigInteger(),
        fee: BigInteger = BigInteger.ZERO,
        spotPrice: BigInteger = BigDecimal("0.500000000000000000").multiply(decimal).toBigInteger(),
        tokenIds: List<BigInteger> = emptyList(),
        value: BigInteger? = null
    ): Pair<Address, Word> {
        val factory = LSSVMPairFactoryV1(sudoswapPairFactory, sender)
        val result = factory.createPairETH(
            nft, // _nft
            bondingCurve, // _bondingCurve
            assetRecipient, // _assetRecipient
            poolType.value.toBigInteger(), // _poolType
            delta, // _delta
            fee, // _fee
            spotPrice, // _spotPrice
            tokenIds.toTypedArray() // _initialNFTIDs
        ).run {
            (if (value != null) this.withValue(value) else this).withSender(sender).execute().verifySuccess()
        }
        val poolAddress = getPoolAddressFromCreateLog(result)
        val orderHash = sudoSwapEventConverter.getPoolHash(poolAddress)
        logger.info("Created pool ($poolAddress), hash=$orderHash")
        return poolAddress to orderHash
    }

    protected suspend fun depositNFTs(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        nft: Address,
        tokenIds: List<BigInteger>
    ) {
        val factory = LSSVMPairFactoryV1(sudoswapPairFactory, sender)
        factory.depositNFTs(
            nft,
            tokenIds.toTypedArray(),
            poolAddress
        ).execute().awaitFirst()
    }

    protected suspend fun swapTokenForAnyNFTs(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        nftCount: Int,
        value: BigInteger
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair.swapTokenForAnyNFTs(
            nftCount.toBigInteger(),
            value,
            sender.from(),
            false,
            Address.ZERO()
        ).withSender(sender).withValue(value).execute().verifySuccess()
    }

    private suspend fun getBuyNFTQuote(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        nftCount: Int,
    ): SudoSwapBuyInfo {
        val pair = LSSVMPairV1(poolAddress, sender)
        val result = pair.getBuyNFTQuote(nftCount.toBigInteger()).call().awaitFirst()
        return SudoSwapBuyInfo(
            newSpotPrice = result._2(),
            newDelta = result._3(),
            inputValue = result._4(),
            protocolFee = result._5()
        )
    }

    protected suspend fun getSingleBuyNFTQuote(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
    ): SudoSwapBuyInfo {
        return getBuyNFTQuote(sender, poolAddress, 1)
    }

    protected suspend fun swapTokenForSpecificNFTs(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        tokenIds: List<BigInteger>,
        value: BigInteger
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair.swapTokenForSpecificNFTs(
            tokenIds.toTypedArray(),
            value,
            sender.from(),
            false,
            Address.ZERO()
        ).withSender(sender).withValue(value).execute().verifySuccess()
    }

    protected suspend fun withdrawERC721(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        token: Address,
        tokenIds: List<BigInteger>
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair.withdrawERC721(
            token,
            tokenIds.toTypedArray()
        ).execute().verifySuccess()
    }

    protected suspend fun changeDelta(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        newDelta: BigInteger
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair.changeDelta(newDelta,).execute().verifySuccess()
    }

    protected suspend fun changeFee(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        newFee: BigInteger
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair.changeFee(newFee).execute().verifySuccess()
    }

    protected suspend fun swapNFTsForToken(
        sender: MonoSigningTransactionSender,
        poolAddress: Address,
        tokenIds: List<BigInteger>,
        minExpectedTokenOutput: BigInteger,
        tokenRecipient: Address,
    ) {
        val pair = LSSVMPairV1(poolAddress, sender)
        pair
            .swapNFTsForToken(tokenIds.toTypedArray(), minExpectedTokenOutput, tokenRecipient, false, Address.ZERO())
            .execute().verifySuccess()
    }

    protected suspend fun checkHoldItems(orderHash: Word, collection: Address, tokenIds: List<BigInteger>) {
        val expectedItemIds = tokenIds.map { ItemId(collection, it).toString() }
        Wait.waitAssert(Duration.ofSeconds(20)) {
            val result = ethereumOrderApi.getAmmOrderItemIds(orderHash.prefixed(), null, null).awaitFirst()
            assertThat(result.ids).containsExactlyInAnyOrderElementsOf(expectedItemIds)
        }
    }

    protected suspend fun checkItemAmmOrderExist(orderHash: Word, collection: Address, tokenIds: List<BigInteger>) {
        Wait.waitAssert(Duration.ofSeconds(20)) {
            coroutineScope {
                tokenIds.map { tokenId ->
                    async {
                        val result = ethereumOrderApi.getAmmOrdersByItem(collection.prefixed(), tokenId.toString(), null, null).awaitFirst()
                        assertThat(result.orders).hasSize(1)
                        assertThat(result.orders.single().hash).isEqualTo(orderHash)
                    }
                }.awaitAll()
            }
        }
    }

    protected suspend fun checkItemAmmOrderNotExist(collection: Address, tokenIds: List<BigInteger>) {
        Wait.waitAssert(Duration.ofSeconds(30)) {
            coroutineScope {
                tokenIds.map { tokenId ->
                    async {
                        val result = ethereumOrderApi.getAmmOrdersByItem(collection.prefixed(), tokenId.toString(), null, null).awaitFirst()
                        logger.info("Found amm orders: $result")
                        assertThat(result.orders).hasSize(0).withFailMessage { "Found orders for $tokenId" }
                    }
                }.awaitAll()
            }
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(
                Request(
                    1, "trace_replayTransaction", Lists.toScala(
                        receipt.transactionHash().toString(),
                        Lists.toScala("trace", "stateDiff")
                    ), "2.0"
                )
            ).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirst()
        logger.info("TxHash: $value")
        require(value != null) { "txHash is null" }

        var attempts = 0
        while (attempts < 20) {
            val result = ethereum.ethGetTransactionReceipt(value).awaitFirst()
            if (result.isDefined) return result.get()
            delay(Duration.ofMillis(500))
            attempts += 1
        }
        throw IllegalStateException("Can't geet Tx $value")
    }

    fun SudoSwapBuyInfo.price(): BigDecimal = this.inputValue.toBigDecimal().divide(PoolCurve.WAD.toBigDecimal()).stripTrailingZeros()

    private fun createEthereumOrderApi(endpoint: String): OrderControllerApi {
        return OrderControllerApi(ApiClient().setBasePath(endpoint))
    }

    companion object {
        val decimal: BigDecimal = BigDecimal.valueOf(10).pow(18)
        val logger: Logger = LoggerFactory.getLogger(SudoSwapTestnetTest::class.java)
    }
}
