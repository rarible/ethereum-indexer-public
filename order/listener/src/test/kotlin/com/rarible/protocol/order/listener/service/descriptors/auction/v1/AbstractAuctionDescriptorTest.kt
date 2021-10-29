package com.rarible.protocol.order.listener.service.descriptors.auction.v1

import com.rarible.contracts.test.erc20.TestERC20
import com.rarible.contracts.test.erc721.TestERC721
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.contracts.auction.v1.AuctionHouse
import com.rarible.protocol.contracts.common.TransferProxy
import com.rarible.protocol.contracts.erc20.proxy.ERC20TransferProxy
import com.rarible.protocol.contracts.royalties.TestRoyaltiesProvider
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.misc.setField
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.FlowPreview
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.com.github.dockerjava.core.dockerfile.DockerfileStatement
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.MonoGasPriceProvider
import scalether.transaction.MonoSigningTransactionSender
import scalether.transaction.MonoSimpleNonceProvider
import java.math.BigInteger

@FlowPreview
abstract class AbstractAuctionDescriptorTest : AbstractIntegrationTest() {
    protected lateinit var userSender1: MonoSigningTransactionSender
    protected lateinit var userSender2: MonoSigningTransactionSender
    protected lateinit var token1: TestERC20
    protected lateinit var token2: TestERC20
    protected lateinit var token721: TestERC721
    protected lateinit var transferProxy: TransferProxy
    protected lateinit var erc20TransferProxy: ERC20TransferProxy
    protected lateinit var auctionHouse: AuctionHouse
    protected lateinit var privateKey1: BigInteger
    protected lateinit var privateKey2: BigInteger

    @Autowired
    private lateinit var auctionCreatedDescriptor: AuctionCreatedDescriptor
    @Autowired
    private lateinit var auctionBidDescriptor: AuctionBidDescriptor
    @Autowired
    private lateinit var auctionFinishedDescriptor: AuctionFinishedDescriptor
    @Autowired
    private lateinit var auctionCancelDescriptor: AuctionCancelDescriptor

    @BeforeEach
    fun before() {
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
        token1 = TestERC20.deployAndWait(sender, poller, "Test1", "TST1").block()!!
        token2 = TestERC20.deployAndWait(sender, poller, "Test2", "TST2").block()!!
        token721 = TestERC721.deployAndWait(sender, poller, "Test", "TST").block()!!
        transferProxy = TransferProxy.deployAndWait(sender, poller).block()!!
        erc20TransferProxy = ERC20TransferProxy.deployAndWait(sender, poller).block()!!
        val royaltiesProvider = TestRoyaltiesProvider.deployAndWait(sender, poller).block()!!
        auctionHouse = AuctionHouse.deployAndWait(sender, poller).block()!!
        auctionHouse.__AuctionHouse_init(
            transferProxy.address(),
            erc20TransferProxy.address(),
            BigInteger.ZERO,
            sender.from(),
            royaltiesProvider.address()
        ).execute().verifySuccess()

        transferProxy.addOperator(auctionHouse.address()).execute().verifySuccess()
        erc20TransferProxy.addOperator(auctionHouse.address()).execute().verifySuccess()
        token1.approve(erc20TransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender1).execute()
            .verifySuccess()
        token2.approve(erc20TransferProxy.address(), BigInteger.TEN.pow(10)).withSender(userSender2).execute()
            .verifySuccess()
        token721.setApprovalForAll(transferProxy.address(), true).withSender(userSender1).execute().verifySuccess()
        token721.setApprovalForAll(transferProxy.address(), true).withSender(userSender2).execute().verifySuccess()

        listOf(
            auctionCreatedDescriptor,
            auctionBidDescriptor,
            auctionFinishedDescriptor,
            auctionCancelDescriptor
        ).forEach(::setAuctionContract)
    }

    private fun setAuctionContract(descriptor: AbstractAuctionDescriptor<*>) {
        setField(descriptor, "auctionContract", auctionHouse.address())
    }

    protected fun mintErc721(from: Address): Erc721AssetType {
        val tokenId = randomBigInt()
        token721.mint(from, tokenId, "test url").execute().verifySuccess()
        return Erc721AssetType(token721.address(), EthUInt256.of(tokenId))
    }
}
