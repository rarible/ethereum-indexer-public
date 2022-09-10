package com.rarible.protocol.order.listener.service.sudoswap.e2e

import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.NewPairEvent
import com.rarible.protocol.dto.AmmOrderDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.gateway.api.ApiClient
import com.rarible.protocol.gateway.api.client.OrderControllerApi
import com.rarible.protocol.order.listener.service.sudoswap.SudoSwapEventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Response
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.mockk
import io.netty.channel.ChannelException
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.time.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
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

abstract class AbstractSudoSwapTestnetTest {
    private val properties = Properties().apply {
        load(Paths.get("src/test/resources/local.properties").inputStream())
    }
    private val ethereumUri = properties["TESTNET_HOST"].toString()
    private val privateKey = Binary.apply(properties["PRIVATE_KEY"].toString())
    protected val sudoswapPairFactory = Address.apply(Binary.apply(properties["SUDOSWAP_PAIR_FACTORY"].toString()))
    protected val sudoswapExponentialCurve = Address.apply(Binary.apply(properties["SUDOSWAP_EXPONENTIAL_CURVE"].toString()))
    protected val sudoswapLinerCurve = Address.apply(Binary.apply(properties["SUDOSWAP_LINER_CURVE"].toString()))
    protected val sudoSwapEventConverter = SudoSwapEventConverter(mockk())

    protected val ethereum = createEthereum(ethereumUri)
    protected val poller = MonoTransactionPoller(ethereum)

    protected val ethereumOrderApi = createEthereumOrderApi(properties["ETHEREUM_API_HOST"].toString())

    protected val userSender = MonoSigningTransactionSender(
        ethereum,
        MonoSimpleNonceProvider(ethereum),
        privateKey.toBigInteger(),
        BigInteger.valueOf(8000000)
    ) { Mono.just(BigInteger.valueOf(800000)) }

    private fun createEthereum(ethereumUri: String): MonoEthereum {
        val requestTimeoutMs = 10000
        val readWriteTimeoutMs  = 10000
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
            .mint(sender.from(), tokenId, "test#$tokenId")
            .execute()
            .verifySuccess()

        return tokenId
    }

    protected fun getPoolAddressFromCreateLog(receipt: TransactionReceipt): Address {
        val logs = receipt.logs()
        val event = logs.find { it.topics().head() == NewPairEvent.id() }.get()
        return NewPairEvent.apply(event).poolAddress()
    }

    protected suspend fun getAmmOrder(hash: Word): AmmOrderDto {
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

    private fun createEthereumOrderApi(endpoint: String): OrderControllerApi {
        return OrderControllerApi(ApiClient().setBasePath(endpoint))
    }

    companion object {
        val decimal: BigDecimal = BigDecimal.valueOf(10).pow(18)
        val logger: Logger = LoggerFactory.getLogger(SudoSwapTestnetTest::class.java)
    }
}