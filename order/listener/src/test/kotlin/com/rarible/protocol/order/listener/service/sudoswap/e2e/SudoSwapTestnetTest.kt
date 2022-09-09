package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Response
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import io.netty.channel.ChannelException
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
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
import java.math.BigInteger
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import kotlin.io.path.inputStream

class SudoSwapTestnetTest {
    private val properties = Properties().apply {
        load(Paths.get("src/test/resources/local.properties").inputStream())
    }
    private val ethereumUri = properties["TESTNET_HOST"].toString()
    private val privateKey = Binary.apply(properties["PRIVATE_KEY"].toString())
    private val sudoswapPairFactory = Address.apply(Binary.apply(properties["SUDOSWAP_PAIR_FACTORY"].toString()))

    private val ethereum = createEthereum(ethereumUri)
    private val poller = MonoTransactionPoller(ethereum)

    private val userSender = MonoSigningTransactionSender(
        ethereum,
        MonoSimpleNonceProvider(ethereum),
        privateKey.toBigInteger(),
        BigInteger.valueOf(8000000)
    ) { Mono.just(BigInteger.valueOf(800000)) }

    @Test
    fun `should create trade pool`() = runBlocking<Unit> {
        val token = createToken(userSender, poller)
        logger.info("Create erc721 token ${token.address()}")

        val result = TestERC721(Address.apply("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"), userSender)
            .mint(userSender.from(), EthUInt256.of(28).value, "test")
            .execute()
            .verifySuccess()

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
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
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
        val logger: Logger = LoggerFactory.getLogger(SudoSwapTestnetTest::class.java)
    }
}

