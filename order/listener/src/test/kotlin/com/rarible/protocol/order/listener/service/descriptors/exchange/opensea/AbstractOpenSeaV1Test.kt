package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.common.opensea.merkle.MerkleValidator
import com.rarible.protocol.contracts.common.wyvern.proxy.WyvernTokenTransferProxy
import com.rarible.protocol.contracts.common.wyvern.registry.WyvernProxyRegistry
import com.rarible.protocol.contracts.common.wyvern.token.TestToken
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.wyvern.WyvernExchange
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.BeforeEach
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

abstract class AbstractOpenSeaV1Test : AbstractIntegrationTest() {

    protected lateinit var userSender1: MonoSigningTransactionSender
    protected lateinit var userSender2: MonoSigningTransactionSender

    protected lateinit var testToken: TestToken
    protected lateinit var wyvernProxyRegistry: WyvernProxyRegistry
    protected lateinit var wyvernTokenTransferProxy: WyvernTokenTransferProxy
    protected lateinit var exchange: WyvernExchange
    protected lateinit var exchangeV2: WyvernExchange
    protected lateinit var merkleValidator: MerkleValidator

    protected lateinit var token1: TestERC20
    protected lateinit var token2: TestERC20
    protected lateinit var token721: TestERC721
    protected lateinit var transferProxy: TransferProxy
    protected lateinit var erc20TransferProxy: ERC20TransferProxy
    protected lateinit var privateKey1: BigInteger
    protected lateinit var privateKey2: BigInteger

    @BeforeEach
    fun before() = runBlocking {
        privateKey1 = Numeric.toBigInt(RandomUtils.nextBytes(32))
        privateKey2 = Numeric.toBigInt(RandomUtils.nextBytes(32))

        userSender1 = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey1,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        userSender2 = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey2,
            BigInteger.valueOf(8000000),
            MonoGasPriceProvider { Mono.just(BigInteger.ZERO) }
        )
        wyvernProxyRegistry = WyvernProxyRegistry.deployAndWait(sender, poller).awaitFirst()
        wyvernTokenTransferProxy = WyvernTokenTransferProxy.deployAndWait(sender, poller, wyvernProxyRegistry.address()).awaitFirst()
        testToken = TestToken.deployAndWait(sender, poller).awaitFirst()

        token1 = TestERC20.deployAndWait(sender, poller, "Test1", "TST1").awaitFirst()
        token2 = TestERC20.deployAndWait(sender, poller, "Test2", "TST2").awaitFirst()
        token721 = TestERC721.deployAndWait(sender, poller, "Test", "TST").awaitFirst()
        transferProxy = TransferProxy.deployAndWait(sender, poller).block()!!
        erc20TransferProxy = ERC20TransferProxy.deployAndWait(sender, poller).block()!!

        exchange = WyvernExchange.deployAndWait(
            sender,
            poller,
            wyvernProxyRegistry.address(),
            wyvernTokenTransferProxy.address(),
            testToken.address(),
            userSender1.from()
        ).awaitFirst()

        exchangeV2 = WyvernExchange.deployAndWait(
            sender,
            poller,
            wyvernProxyRegistry.address(),
            wyvernTokenTransferProxy.address(),
            testToken.address(),
            userSender1.from()
        ).awaitFirst()

        merkleValidator = MerkleValidator.deployAndWait(
            sender,
            poller
        ).awaitFirst()

        exchangeContractAddresses.openSeaV1 = exchange.address()
        exchangeContractAddresses.openSeaV2 = exchangeV2.address()

        wyvernProxyRegistry.grantInitialAuthentication(exchange.address()).execute().verifySuccess()

        wyvernProxyRegistry.registerProxy().withSender(userSender1).execute().awaitFirst()
        val user1RegisteredProxy = wyvernProxyRegistry.proxies(userSender1.from()).awaitFirst()

        token1.approve(wyvernTokenTransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender1).execute().verifySuccess()
        token1.approve(wyvernTokenTransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender2).execute().verifySuccess()
        token2.approve(wyvernTokenTransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender2).execute().verifySuccess()
        token721.setApprovalForAll(user1RegisteredProxy, true).withSender(userSender1).execute().verifySuccess()

        logger.info("User1: ${userSender1.from()}")
        logger.info("User2: ${userSender2.from()}")

        logger.info("WyvernProxyRegistry: ${wyvernProxyRegistry.address()}")
        logger.info("WyvernTokenTransferProxy: ${wyvernTokenTransferProxy.address()}")
        logger.info("TestToken: ${testToken.address()}")

        logger.info("Token1: ${token1.address()}")
        logger.info("Token2: ${token2.address()}")

        logger.info("Token721: ${token721.address()}")

        logger.info("TransferProxy: ${transferProxy.address()}")
        logger.info("Erc20TransferProxy: ${erc20TransferProxy.address()}")
        logger.info("Exchange: ${exchange.address()}")
    }

    companion object {
        val WORD_ZERO: Word = Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000")
    }
}
