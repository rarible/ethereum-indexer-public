package com.rarible.protocol.nft.listener.test

import com.rarible.blockchain.scanner.block.BlockRepository
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.repository.history.NftHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.token.TokenProvider
import com.rarible.protocol.nft.core.service.token.TokenReduceService
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.core.test.TestKafkaHandler
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.query.Query
import org.web3jold.crypto.Keys
import org.web3jold.crypto.Sign
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.response.TransactionReceipt
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger
import java.time.Instant
import javax.annotation.PostConstruct

abstract class AbstractIntegrationTest {

    protected lateinit var sender: MonoTransactionSender

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var mongoConverter: MongoConverter

    @Autowired
    protected lateinit var ethereum: MonoEthereum

    @Autowired
    protected lateinit var poller: MonoTransactionPoller

    @Autowired
    protected lateinit var tokenRepository: TokenRepository

    @Autowired
    protected lateinit var itemRepository: ItemRepository

    @Autowired
    protected lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    protected lateinit var nftItemHistoryRepository: NftItemHistoryRepository

    @Autowired
    protected lateinit var nftHistoryRepository: NftHistoryRepository

    @Autowired
    protected lateinit var nftIndexerProperties: NftIndexerProperties

    @Autowired
    protected lateinit var featureFlags: FeatureFlags

    @Autowired(required = false)
    protected var blockRepository: BlockRepository? = null

    @Autowired
    private lateinit var testActivityHandler: TestKafkaHandler<EthActivityEventDto>

    @Autowired
    protected lateinit var tokenProvider: TokenProvider

    @Autowired
    protected lateinit var reindexTokenService: ReindexTokenService

    @Autowired
    protected lateinit var tokenService: TokenService

    @Autowired
    protected lateinit var tokenReduceService: TokenReduceService

    @BeforeEach
    fun cleanDatabase() = runBlocking<Unit> {
        testActivityHandler.events.clear()

        mongo.collectionNames
            .filter { !it.startsWith("system") && !it.equals("block") }
            .flatMap { mongo.remove(Query(), it) }
            .then().block()
    }

    @BeforeEach
    fun markCurrentBlockSuccessfullyHandled() = runBlocking<Unit> {
    }

    @PostConstruct
    fun setUp() {
        sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            Numeric.toBigInt("0x0a2853fac2c0a03f463f04c4567839473c93f3307da459132b7dd1ca633c0e16"),
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success())
        return receipt
    }

    protected fun newSender(privateKey0: BigInteger? = null): Pair<Address, MonoSigningTransactionSender> {
        val (privateKey, _, address) = generateNewKeys(privateKey0)
        val sender = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }
        return address to sender
    }

    private fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    protected suspend fun TransactionReceipt.getTimestamp(): Instant =
        Instant.ofEpochSecond(ethereum.ethGetFullBlockByHash(blockHash()).map { it.timestamp() }.awaitFirst().toLong())

    protected suspend fun checkActivityWasPublished(
        token: Address,
        tokenId: EthUInt256,
        topic: Word,
        activityType: Class<out NftActivityDto>
    ) = coroutineScope {
        val logEvent = nftItemHistoryRepository.findItemsHistory(token, tokenId)
            .filter { it.log.topic == topic }
            .map { it.log }
            .awaitFirstOrNull()

        assertThat(logEvent).isNotNull

        val events = testActivityHandler.events

        Wait.waitAssert {
            assertThat(events).hasSizeGreaterThanOrEqualTo(1)
            val activity = events.find { event -> event.activity.id == logEvent?.id.toString() }?.activity
            assertThat(activity).isNotNull
            assertThat(activity?.javaClass).isEqualTo(activityType)
            when (activity) {
                is MintDto -> {
                    assertThat(activity.id).isEqualTo(logEvent?.id.toString())
                    assertThat(activity.contract).isEqualTo(token)
                    assertThat(activity.tokenId).isEqualTo(tokenId.value)
                }
                is TransferDto -> {
                    assertThat(activity.id).isEqualTo(logEvent?.id.toString())
                    assertThat(activity.contract).isEqualTo(token)
                    assertThat(activity.tokenId).isEqualTo(tokenId.value)
                }
                else -> Assertions.fail<String>("Unexpected event type ${activity?.javaClass}")
            }
        }
    }
}
