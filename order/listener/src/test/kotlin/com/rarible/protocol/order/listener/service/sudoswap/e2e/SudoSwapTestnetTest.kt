package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.contracts.exchange.sudoswap.v1.pair.LSSVMPairV1
import com.rarible.protocol.order.core.model.SudoSwapPoolType
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Response
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import io.netty.channel.ChannelException
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
import java.util.*
import kotlin.io.path.inputStream

@Disabled("This is manual test")
class SudoSwapTestnetTest {
    private val properties = Properties().apply {
        load(Paths.get("src/test/resources/local.properties").inputStream())
    }
    private val ethereumUri = properties["TESTNET_HOST"].toString()
    private val privateKey = Binary.apply(properties["PRIVATE_KEY"].toString())
    private val sudoswapPairFactory = Address.apply(Binary.apply(properties["SUDOSWAP_PAIR_FACTORY"].toString()))
    private val sudoswapExponentialCurve = Address.apply(Binary.apply(properties["SUDOSWAP_EXPONENTIAL_CURVE"].toString()))
    private val sudoswapLinerCurve = Address.apply(Binary.apply(properties["SUDOSWAP_LINER_CURVE"].toString()))

    private val ethereum = createEthereum(ethereumUri)
    private val poller = MonoTransactionPoller(ethereum)

    private val userSender = MonoSigningTransactionSender(
        ethereum,
        MonoSimpleNonceProvider(ethereum),
        privateKey.toBigInteger(),
        BigInteger.valueOf(8000000)
    ) { Mono.just(BigInteger.valueOf(800000)) }

    @Test
    fun `should create trade pool with liner curve`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        logger.info("Create erc721 token ${token.address()}")

        val tokenId1 = BigInteger.valueOf(1)
        val tokenId2 = BigInteger.valueOf(2)

        token
            .mint(userSender.from(), tokenId1, "test#$tokenId1")
            .execute()
            .verifySuccess()

        token
            .mint(userSender.from(), tokenId2, "test#$tokenId2")
            .execute()
            .verifySuccess()

        token.approve(sudoswapPairFactory, tokenId1).execute().verifySuccess()
        token.approve(sudoswapPairFactory, tokenId2).execute().verifySuccess()

        val factory = LSSVMPairFactoryV1(sudoswapPairFactory, userSender)
        /**
         * def createPairETH(
         *  _nft: Address,
         *  _bondingCurve: Address,
         *  _assetRecipient: Address,
         *  _poolType: BigInteger,
         *  _delta: BigInteger,
         *  _fee: BigInteger,
         *  _spotPrice: BigInteger,
         *  _initialNFTIDs: Array[BigInteger]
         * )
         */
        val result = factory.createPairETH(
            token.address(),
            sudoswapLinerCurve,
            userSender.from(),
            SudoSwapPoolType.NFT.value.toBigInteger(),
            BigDecimal.valueOf(0.2).multiply(decimal).toBigInteger(),
            BigInteger.ZERO,
            BigDecimal.valueOf(0.5).multiply(decimal).toBigInteger(),
            arrayOf(tokenId1)
        ).execute().verifySuccess()

        val logs = result.logs()
        val event = logs.find { it.topics().head() == NewPairEvent.id() }.get()
        val poolAddress = NewPairEvent.apply(event).poolAddress()
        logger.info("Pool $poolAddress was created")

        factory.depositNFTs(
            token.address(),
            arrayOf(tokenId2),
            poolAddress
        ).execute().awaitFirst()
    }

    private suspend fun createToken(
        sender: MonoSigningTransactionSender,
        poller: MonoTransactionPoller
    ): TestERC721 {
        return TestERC721.deployAndWait(sender, poller, "ipfs:/", "test").awaitFirst()
    }

    private fun createEthereum(ethereumUri: String): MonoEthereum {
        val requestTimeoutMs = 10000
        val readWriteTimeoutMs  = 10000
        val maxFrameSize = 1024 * 1024
        val retryMaxAttempts = 5L
        val retryBackoffDelay = 100L

        val retry = Retry
            .backoff(retryMaxAttempts, Duration.ofMillis(retryBackoffDelay))
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

    private suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(Request(1, "trace_replayTransaction", Lists.toScala(
                receipt.transactionHash().toString(),
                Lists.toScala("trace", "stateDiff")
            ), "2.0")).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    companion object {
        val decimal: BigDecimal = BigDecimal.valueOf(10).pow(18)
        val logger: Logger = LoggerFactory.getLogger(SudoSwapTestnetTest::class.java)
    }
}

