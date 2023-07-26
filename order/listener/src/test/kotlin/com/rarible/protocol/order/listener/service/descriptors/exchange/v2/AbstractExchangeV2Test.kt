package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.contracts.test.erc1155.TestERC1155
import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.exchange.v2.rev3.ExchangeV2
import com.rarible.protocol.contracts.royalties.TestRoyaltiesProvider
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.coEvery
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.web3jold.utils.Numeric
import reactor.core.publisher.Mono
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2 as LegacyExchangeV2

abstract class AbstractExchangeV2Test : AbstractIntegrationTest() {
    protected lateinit var userSender1: MonoSigningTransactionSender
    protected lateinit var userSender2: MonoSigningTransactionSender
    protected lateinit var token1: TestERC20
    protected lateinit var token2: TestERC20
    protected lateinit var token721: TestERC721
    protected lateinit var token1155: TestERC1155
    protected lateinit var transferProxy: TransferProxy
    protected lateinit var erc20TransferProxy: ERC20TransferProxy
    protected lateinit var legacyExchange: LegacyExchangeV2
    protected lateinit var exchange: ExchangeV2
    protected lateinit var legacyEip712Domain: EIP712Domain
    protected lateinit var eip712Domain: EIP712Domain
    protected lateinit var privateKey1: BigInteger
    protected lateinit var privateKey2: BigInteger
    @Autowired
    protected lateinit var prepareTxService: PrepareTxService
    @Autowired
    protected lateinit var erc721SignService: ERC1271SignService
    @Autowired
    protected lateinit var orderDtoConverter: OrderDtoConverter

    @BeforeEach
    fun before() {
        privateKey1 = Numeric.toBigInt(RandomUtils.nextBytes(32))
        privateKey2 = Numeric.toBigInt(RandomUtils.nextBytes(32))

        userSender1 = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey1,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        userSender2 = MonoSigningTransactionSender(
            ethereum,
            MonoSimpleNonceProvider(ethereum),
            privateKey2,
            BigInteger.valueOf(8000000)
        ) { Mono.just(BigInteger.ZERO) }

        token1 = TestERC20.deployAndWait(sender, poller, "Test1", "TST1").block()!!
        token2 = TestERC20.deployAndWait(sender, poller, "Test2", "TST2").block()!!
        token721 = TestERC721.deployAndWait(sender, poller, "Test", "TST").block()!!
        token1155 = TestERC1155.deployAndWait(sender, poller, "ipfs:/").block()!!
        transferProxy = TransferProxy.deployAndWait(sender, poller).block()!!
        erc20TransferProxy = ERC20TransferProxy.deployAndWait(sender, poller).block()!!
        val royaltiesProvider = TestRoyaltiesProvider.deployAndWait(sender, poller).block()!!
        legacyExchange = LegacyExchangeV2.deployAndWait(sender, poller).block()!!
        exchange = ExchangeV2.deployAndWait(sender, poller).block()!!

        legacyExchange.__ExchangeV2_init(
            transferProxy.address(),
            erc20TransferProxy.address(),
            BigInteger.ZERO,
            sender.from(),
            royaltiesProvider.address()
        ).execute().verifySuccess()

        exchange.__ExchangeV2_init(
            transferProxy.address(),
            erc20TransferProxy.address(),
            BigInteger.ZERO,
            sender.from(),
            royaltiesProvider.address()
        ).execute().verifySuccess()

        transferProxy.addOperator(legacyExchange.address()).execute().verifySuccess()
        erc20TransferProxy.addOperator(legacyExchange.address()).execute().verifySuccess()

        transferProxy.addOperator(exchange.address()).execute().verifySuccess()
        erc20TransferProxy.addOperator(exchange.address()).execute().verifySuccess()

        token1.approve(erc20TransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender1).execute()
            .verifySuccess()
        token2.approve(erc20TransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender2).execute()
            .verifySuccess()
        token721.setApprovalForAll(transferProxy.address(), true).withSender(userSender1).execute().verifySuccess()
        token721.setApprovalForAll(transferProxy.address(), true).withSender(userSender2).execute().verifySuccess()
        token1155.setApprovalForAll(transferProxy.address(), true).withSender(userSender1).execute().verifySuccess()
        token1155.setApprovalForAll(transferProxy.address(), true).withSender(userSender2).execute().verifySuccess()

        legacyEip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = legacyExchange.address()
        )
        eip712Domain = EIP712Domain(
            name = "Exchange",
            version = "2",
            chainId = BigInteger.valueOf(17),
            verifyingContract = exchange.address()
        )
        clearMocks(erc721SignService)
        coEvery { erc721SignService.isSigner(any(), any() as Word, any()) } returns true
    }
}
