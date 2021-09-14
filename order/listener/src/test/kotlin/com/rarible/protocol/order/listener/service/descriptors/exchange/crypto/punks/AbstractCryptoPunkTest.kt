package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderPriceHistoryRecord
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import java.time.Instant
import java.util.*

@FlowPreview
abstract class AbstractCryptoPunkTest : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @Autowired
    protected lateinit var prepareTxService: PrepareTxService

    protected lateinit var cryptoPunksMarket: CryptoPunksMarket

    @BeforeEach
    fun clearKafkaQueue() {
        // Wait for a while to clean up the Kafka queue.
        // TODO: invent a better way of cleaning up Kafka (use a testing library?)
        Thread.sleep(400)
        checkPublishedActivities { }
    }

    @BeforeEach
    fun initializeCryptoPunksMarket() = runBlocking {
        cryptoPunksMarket = deployCryptoPunkMarket()

        // Override asset make balance service to correctly reflect ownership of CryptoPunks.
        // By default, this service returns 1 for all ownerships, even if a punk does not belong to this address.
        assetMakeBalanceAnswers = r@ { order ->
            val assetType = order.make.type as? CryptoPunksAssetType ?: return@r null
            if (assetType.marketAddress != cryptoPunksMarket.address()) {
                return@r null
            }
            val realOwner = cryptoPunksMarket.punkIndexToAddress(assetType.punkId.toBigInteger()).awaitSingle()
            if (order.maker == realOwner) EthUInt256.ONE else EthUInt256.ZERO
        }
    }

    protected fun checkActivityWasPublished(predicate: ActivityDto.() -> Boolean) =
        checkPublishedActivities { activities ->
            Assertions.assertTrue(activities.any(predicate)) {
                "Searched-for activity is not found in\n" + activities.joinToString("\n")
            }
        }

    protected fun checkPublishedActivities(assertBlock: suspend (List<ActivityDto>) -> Unit) = runBlocking {
        val activities = Collections.synchronizedList(arrayListOf<ActivityDto>())
        val job = async {
            consumer.receive().collect { activities.add(it.value) }
        }
        try {
            Wait.waitAssert {
                assertBlock(activities)
            }
        } finally {
            job.cancelAndJoin()
        }
    }

    protected fun depositInitialBalance(to: Address, amount: BigInteger) {
        val coinBaseWalletPrivateKey =
            BigInteger(Numeric.hexStringToByteArray("00120de4b1518cf1f16dc1b02f6b4a8ac29e870174cb1d8575f578480930250a"))
        val (coinBaseAddress, coinBaseSender) = newSender(coinBaseWalletPrivateKey)
        coinBaseSender.sendTransaction(
            Transaction(
                to,
                coinBaseAddress,
                BigInteger.valueOf(8000000),
                BigInteger.ZERO,
                amount,
                Binary(ByteArray(1)),
                null
            )
        ).verifySuccess()
    }

    protected suspend fun getEthBalance(account: Address): BigInteger =
        ethereum.ethGetBalance(account, "latest").awaitFirst()

    protected suspend fun deployCryptoPunkMarket(): CryptoPunksMarket {
        val (_, creatorSender) = newSender()
        val market = CryptoPunksMarket.deployAndWait(creatorSender, poller).awaitFirst()
        market.allInitialOwnersAssigned().execute().awaitFirst()
        exchangeContractAddresses.cryptoPunks = market.address()
        return market
    }

    protected fun newSender(privateKey0: BigInteger? = null): Triple<Address, MonoSigningTransactionSender, BigInteger> {
        val (privateKey, _, address) = generateNewKeys(privateKey0)
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        return Triple(address, sender, privateKey)
    }

    protected fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    protected fun createPriceHistory(time: Instant, make: Asset, take: Asset) =
        listOf(
            OrderPriceHistoryRecord(
                date = time,
                makeValue = make.value.value.toBigDecimal(if (make.type == EthAssetType) 18 else 0),
                takeValue = take.value.value.toBigDecimal(if (take.type == EthAssetType) 18 else 0)
            )
        )
}
