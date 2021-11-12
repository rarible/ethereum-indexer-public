package com.rarible.protocol.nft.api.e2e.pending

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC1155RaribleUserEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleEvent
import com.rarible.protocol.contracts.collection.CreateERC721RaribleUserEvent
import com.rarible.protocol.contracts.erc1155.rarible.ERC1155Rarible
import com.rarible.protocol.contracts.erc1155.rarible.factory.ERC1155RaribleFactoryC2
import com.rarible.protocol.contracts.erc1155.rarible.factory.beacon.ERC1155RaribleBeacon
import com.rarible.protocol.contracts.erc1155.rarible.factory.user.ERC1155RaribleUserFactoryC2
import com.rarible.protocol.contracts.erc1155.rarible.user.ERC1155RaribleUser
import com.rarible.protocol.contracts.erc721.rarible.ERC721Rarible
import com.rarible.protocol.contracts.erc721.rarible.factory.ERC721RaribleFactoryC2
import com.rarible.protocol.contracts.erc721.rarible.factory.beacon.ERC721RaribleBeaconMinimal
import com.rarible.protocol.contracts.erc721.rarible.factory.user.ERC721RaribleUserFactoryC2
import com.rarible.protocol.contracts.erc721.rarible.user.ERC721RaribleUserMinimal
import com.rarible.protocol.dto.CreateTransactionRequestDto
import com.rarible.protocol.dto.LogEventDto
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import io.daonomic.rpc.domain.Request
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils.setField
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Transaction
import scalether.domain.response.TransactionReceipt
import scalether.java.Lists
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import scalether.transaction.MonoTransactionSender
import java.math.BigInteger

@End2EndTest
class FactoryPendingTransactionFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var nftIndexerProperties: NftIndexerProperties

    private lateinit var userSender: MonoTransactionSender

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
    }

    @Test
    fun `should create log for ERC721RaribleFactory`() = runBlocking<Unit> {
        val transferAddress = randomAddress()
        val lazyAddress = randomAddress()
        val token = ERC721Rarible.deployAndWait(userSender, poller).awaitSingle()
        val beacon = ERC721RaribleBeaconMinimal.deployAndWait(userSender, poller, token.address()).awaitSingle()
        val factory = ERC721RaribleFactoryC2.deployAndWait(userSender, poller,
            beacon.address(), transferAddress, lazyAddress).awaitSingle()
        setField(nftIndexerProperties, "factory", NftIndexerProperties.FactoryAddresses(
            erc721Rarible = factory.address().hex(),
            erc721RaribleUser = randomAddress().hex(),
            erc1155Rarible = randomAddress().hex(),
            erc1155RaribleUser = randomAddress().hex()
        ))

        val receipt = factory.createToken("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).execute().verifySuccess()
        val contract = factory.getAddress("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).awaitSingle()

        processTransaction(receipt, contract, CreateERC721RaribleEvent.id())
    }

    @Test
    fun `should create log for ERC721RaribleUserFactory`() = runBlocking<Unit> {
        val token = ERC721RaribleUserMinimal.deployAndWait(userSender, poller).awaitSingle()
        val beacon = ERC721RaribleBeaconMinimal.deployAndWait(userSender, poller, token.address()).awaitSingle()
        val factory = ERC721RaribleUserFactoryC2.deployAndWait(userSender, poller, beacon.address()).awaitSingle()
        setField(nftIndexerProperties, "factory", NftIndexerProperties.FactoryAddresses(
            erc721Rarible = randomAddress().hex(),
            erc721RaribleUser = factory.address().hex(),
            erc1155Rarible = randomAddress().hex(),
            erc1155RaribleUser = randomAddress().hex()
        ))

        val receipt = factory.createToken("NAME", "SYMBOL", "uri", "uri", emptyArray(), BigInteger.ONE).execute().verifySuccess()
        val contract = factory.getAddress("NAME", "SYMBOL", "uri", "uri", emptyArray(), BigInteger.ONE).awaitSingle()

        processTransaction(receipt, contract, CreateERC721RaribleUserEvent.id())
    }

    @Test
    fun `should create log for ERC1155RaribleFactory`() = runBlocking<Unit> {
        val transferAddress = randomAddress()
        val lazyAddress = randomAddress()
        val token = ERC1155Rarible.deployAndWait(userSender, poller).awaitSingle()
        val beacon = ERC1155RaribleBeacon.deployAndWait(userSender, poller, token.address()).awaitSingle()
        val factory = ERC1155RaribleFactoryC2.deployAndWait(userSender, poller,
            beacon.address(), transferAddress, lazyAddress).awaitSingle()
        setField(nftIndexerProperties, "factory", NftIndexerProperties.FactoryAddresses(
            erc721Rarible = randomAddress().hex(),
            erc721RaribleUser = randomAddress().hex(),
            erc1155Rarible = factory.address().hex(),
            erc1155RaribleUser = randomAddress().hex()
        ))

        val receipt = factory.createToken("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).execute().verifySuccess()
        val contract = factory.getAddress("NAME", "SYMBOL", "uri", "uri", BigInteger.ONE).awaitSingle()

        processTransaction(receipt, contract, CreateERC1155RaribleEvent.id())
    }

    @Test
    fun `should create log for ERC1155RaribleUserFactory`() = runBlocking<Unit> {
        val token = ERC1155RaribleUser.deployAndWait(userSender, poller).awaitSingle()
        val beacon = ERC1155RaribleBeacon.deployAndWait(userSender, poller, token.address()).awaitSingle()
        val factory = ERC1155RaribleUserFactoryC2.deployAndWait(userSender, poller,
            beacon.address()).awaitSingle()
        setField(nftIndexerProperties, "factory", NftIndexerProperties.FactoryAddresses(
            erc721Rarible = randomAddress().hex(),
            erc721RaribleUser = randomAddress().hex(),
            erc1155Rarible = randomAddress().hex(),
            erc1155RaribleUser = factory.address().hex()
        ))

        val receipt = factory.createToken("NAME", "SYMBOL", "uri", "uri", emptyArray(), BigInteger.ONE).execute().verifySuccess()
        val contract = factory.getAddress("NAME", "SYMBOL", "uri", "uri", emptyArray(), BigInteger.ONE).awaitSingle()

        processTransaction(receipt, contract, CreateERC1155RaribleUserEvent.id())
    }

    private suspend fun processTransaction(receipt: TransactionReceipt, contract: Address, id: Word) {
        val tx = ethereum.ethGetTransactionByHash(receipt.transactionHash()).awaitFirst().get()

        val eventLogs = nftTransactionApiClient.createNftPendingTransaction(tx.toRequest()).collectList().awaitFirst()

        assertThat(eventLogs).hasSize(1)

        with(eventLogs.single()) {
            assertThat(transactionHash).isEqualTo(tx.hash())
            assertThat(address).isEqualTo(contract)
            assertThat(topic).isEqualTo(id)
            assertThat(status).isEqualTo(LogEventDto.Status.PENDING)
        }
    }

    protected suspend fun Mono<Word>.verifySuccess(): TransactionReceipt {
        val receipt = waitReceipt()
        Assertions.assertTrue(receipt.success()) {
            val result = ethereum.executeRaw(
                Request(1, "trace_replayTransaction", Lists.toScala(
                    receipt.transactionHash().toString(),
                    Lists.toScala("trace", "stateDiff")
                ), "2.0")
            ).block()!!
            "traces: ${result.result().get()}"
        }
        return receipt
    }

    private suspend fun Mono<Word>.waitReceipt(): TransactionReceipt {
        val value = this.awaitFirstOrNull()
        require(value != null) { "txHash is null" }
        return ethereum.ethGetTransactionReceipt(value).awaitFirst().get()
    }

    private fun Transaction.toRequest() = CreateTransactionRequestDto(
        hash = hash(),
        from = from(),
        input = input(),
        nonce = nonce().toLong(),
        to = to()
    )

}
