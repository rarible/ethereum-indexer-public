package com.rarible.protocol.order.core.service.approve

import com.rarible.contracts.erc721.IERC721
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBoolean
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721Type
import com.rarible.protocol.order.core.metric.ApprovalMetrics
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scala.Tuple2
import scalether.abi.BoolType
import scalether.domain.Address
import scalether.domain.request.Transaction
import scalether.transaction.ReadOnlyMonoTransactionSender

internal class ApproveServiceTest {

    private val erc20Service = mockk<Erc20Service>()
    private val transferProxyAddresses = randomProxyAddresses()
    private val approveRepository = mockk<ApprovalHistoryRepository>()
    private val sender = mockk<ReadOnlyMonoTransactionSender>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val approvalMetrics = mockk<ApprovalMetrics> {
        every { onApprovalEventMiss(any()) } returns Unit
        every { onApprovalOnChainCheck(any(), any()) } returns Unit
    }
    private val approveService = ApproveService(
        erc20Service,
        approveRepository,
        featureFlags,
        sender,
        approvalMetrics,
        transferProxyAddresses
    )

    @Test
    fun `should approve for rarible platform`() = runBlocking<Unit> {
        val maker = randomAddress()
        val collection = randomAddress()
        val proxy = transferProxyAddresses.transferProxy
        val expectedApproval = randomBoolean()
        mockGetLogEvent(collection, maker, proxy, expectedApproval)
        val result = approveService.checkApprove(maker, collection, Platform.RARIBLE)
        assertThat(result).isEqualTo(expectedApproval)
        coVerify { approveRepository.lastApprovalLogEvent(collection, maker, proxy) }
    }

    @Test
    fun `should approve for x2y2 platforms erc721`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc721,
            listOf(transferProxyAddresses.x2y2TransferProxyErc1155)
        )
    }

    @Test
    fun `should approve for x2y2 platforms erc1155`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc1155,
            listOf(transferProxyAddresses.x2y2TransferProxyErc721)
        )
    }

    @Test
    fun `should approve for seaport platforms`() = runBlocking<Unit> {
        testApproval(Platform.OPEN_SEA, transferProxyAddresses.seaportTransferProxy)
    }

    @Test
    fun `should approve for crypto punks platforms`() = runBlocking<Unit> {
        testApproval(Platform.CRYPTO_PUNKS, transferProxyAddresses.cryptoPunksTransferProxy)
    }

    @Test
    fun `should approve for looksrare erc721`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareTransferManagerERC721,
            transferProxyAddresses.looksrareTransferManagerERC1155,
            transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            transferProxyAddresses.looksrareV2TransferManager
        )
    }

    @Test
    fun `should approve for looksrare non erc721`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            transferProxyAddresses.looksrareTransferManagerERC1155,
            transferProxyAddresses.looksrareTransferManagerERC721,
            transferProxyAddresses.looksrareV2TransferManager
        )
    }

    @Test
    fun `should approve for looksrare erc1155`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareTransferManagerERC1155,
            transferProxyAddresses.looksrareTransferManagerERC721,
            transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            transferProxyAddresses.looksrareV2TransferManager
        )
    }

    @Test
    fun `should approve for looksrare V2`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareV2TransferManager,
            transferProxyAddresses.looksrareTransferManagerERC1155,
            transferProxyAddresses.looksrareTransferManagerERC721,
            transferProxyAddresses.looksrareTransferManagerNonCompliantERC721
        )
    }

    @Test
    fun `should check rarible on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(Platform.RARIBLE, transferProxyAddresses.transferProxy)
    }

    @Test
    fun `should check seaport on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(Platform.OPEN_SEA, transferProxyAddresses.seaportTransferProxy)
    }

    @Test
    fun `should check x2y2 on chain approve erc721`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc721,
            listOf(transferProxyAddresses.x2y2TransferProxyErc1155)
        )
    }

    @Test
    fun `should check x2y2 on chain approve erc1155`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.X2Y2,
            transferProxyAddresses.x2y2TransferProxyErc1155,
            listOf(transferProxyAddresses.x2y2TransferProxyErc721)
        )
    }

    @Test
    fun `should check punks on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(Platform.CRYPTO_PUNKS, transferProxyAddresses.cryptoPunksTransferProxy)
    }

    @Test
    fun `should check looksrare erc721 on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.LOOKSRARE,
            transferProxyAddresses.looksrareTransferManagerERC721,
            otherProxies = listOf(
                transferProxyAddresses.looksrareTransferManagerERC1155,
                transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
                transferProxyAddresses.looksrareV2TransferManager
            )
        )
    }

    @Test
    fun `should check looksrare erc1155 on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.LOOKSRARE,
            transferProxyAddresses.looksrareTransferManagerERC1155,
            listOf(
                transferProxyAddresses.looksrareTransferManagerERC721,
                transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
                transferProxyAddresses.looksrareV2TransferManager
            )
        )
    }

    @Test
    fun `should check looksrare non erc721 on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.LOOKSRARE,
            transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            listOf(
                transferProxyAddresses.looksrareTransferManagerERC721,
                transferProxyAddresses.looksrareTransferManagerERC1155,
                transferProxyAddresses.looksrareV2TransferManager
            )
        )
    }

    @Test
    fun `should check looksrare V2 on chain approve`() = runBlocking<Unit> {
        testOnChainApproval(
            Platform.LOOKSRARE,
            transferProxyAddresses.looksrareV2TransferManager,
            listOf(
                transferProxyAddresses.looksrareTransferManagerERC721,
                transferProxyAddresses.looksrareTransferManagerERC1155,
                transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            )
        )
    }

    @Test
    fun `should not check on chain`() = runBlocking<Unit> {
        every { featureFlags.checkOnChainApprove } returns false
        val result = approveService.checkOnChainApprove(randomAddress(), randomErc721Type(), Platform.RARIBLE)
        assertThat(result).isTrue
        verify(exactly = 0) { sender.call(any()) }
    }

    @Test
    fun `should not check on chain if not nft asset`() = runBlocking<Unit> {
        every { featureFlags.checkOnChainApprove } returns true
        val result = approveService.checkOnChainApprove(randomAddress(), randomErc20().type, Platform.RARIBLE)
        assertThat(result).isTrue
        verify(exactly = 0) { sender.call(any()) }
    }

    @Test
    fun `should not apply on chain check`() = runBlocking<Unit> {
        every { featureFlags.checkOnChainApprove } returns true
        every { featureFlags.applyOnChainApprove } returns false

        val maker = randomAddress()
        val token = randomAddress()
        mockkSender(token, maker, transferProxyAddresses.transferProxy, false)
        val result = approveService.checkOnChainApprove(maker, randomErc721Type(token), Platform.RARIBLE)
        assertThat(result).isTrue
        verify(exactly = 1) { sender.call(any()) }
    }

    @Test
    fun `should apply on chain check`() = runBlocking<Unit> {
        every { featureFlags.checkOnChainApprove } returns true
        every { featureFlags.applyOnChainApprove } returns true

        val maker = randomAddress()
        val token = randomAddress()
        mockkSender(token, maker, transferProxyAddresses.transferProxy, false)
        val result = approveService.checkOnChainApprove(maker, randomErc721Type(token), Platform.RARIBLE)
        assertThat(result).isFalse
    }

    private fun `should approve for looksrare`(
        proxyWithEvent: Address,
        vararg proxyWithNoEvents: Address
    ) = runBlocking<Unit> {
        val maker = randomAddress()
        val token = randomAddress()
        val approved = randomBoolean()

        mockGetLogEvent(token, maker, proxyWithEvent, approved)
        proxyWithNoEvents.forEach {
            mockGetLogEvent(token, maker, it, null)
        }
        val result = approveService.checkApprove(maker, token, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(approved)
        coVerify { approveRepository.lastApprovalLogEvent(token, maker, proxyWithEvent) }
        proxyWithNoEvents.forEach {
            coVerify { approveRepository.lastApprovalLogEvent(token, maker, it) }
        }
    }

    private suspend fun testApproval(platform: Platform, proxy: Address) {
        val maker = randomAddress()
        val expectedApproval = randomBoolean()
        val token = randomAddress()
        mockGetLogEvent(token, maker, proxy, expectedApproval)
        val result = approveService.checkApprove(maker, token, platform)
        assertThat(result).isEqualTo(expectedApproval)
        coVerify { approveRepository.lastApprovalLogEvent(token, maker, proxy) }
    }

    private suspend fun testOnChainApproval(platform: Platform, proxy: Address) {
        val maker = randomAddress()
        val token = randomAddress()
        val approved = randomBoolean()
        mockkSender(token, maker, proxy, approved)
        val result = approveService.checkOnChainApprove(maker, token, platform)
        assertThat(result).isEqualTo(approved)
    }

    private suspend fun testOnChainApproval(
        platform: Platform,
        approved: Address,
        otherProxies: List<Address>,
    ) {
        val maker = randomAddress()
        val token = randomAddress()
        mockkSender(token, maker, approved, true)
        otherProxies.forEach {
            mockkSender(token, maker, it, false)
        }
        val result = approveService.checkOnChainApprove(maker, token, platform)
        assertThat(result).isEqualTo(true)
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    private fun mockkSender(token: Address, maker: Address, operator: Address, approved: Boolean) {
        coEvery {
            sender.call(
                Transaction(
                    token,
                    null,
                    null,
                    null,
                    null,
                    IERC721.isApprovedForAllSignature().encode(Tuple2(maker, operator)),
                    null
                )
            )
        } returns Mono.just(BoolType.encode(approved))
    }

    private fun mockGetLogEvent(collection: Address, maker: Address, proxy: Address, expectedApproval: Boolean?) {
        coEvery {
            approveRepository.lastApprovalLogEvent(
                collection,
                maker,
                proxy
            )
        } returns expectedApproval?.let { logEvent(it) }
    }

    private fun logEvent(approved: Boolean) = createLogEvent(randomApproveHistory(approved = approved))

    private fun randomProxyAddresses() = OrderIndexerProperties.TransferProxyAddresses(
        transferProxy = randomAddress(),
        erc20TransferProxy = randomAddress(),
        erc721LazyTransferProxy = randomAddress(),
        erc1155LazyTransferProxy = randomAddress(),
        cryptoPunksTransferProxy = randomAddress(),
        seaportTransferProxy = randomAddress(),
        looksrareTransferManagerERC721 = randomAddress(),
        looksrareTransferManagerERC1155 = randomAddress(),
        looksrareTransferManagerNonCompliantERC721 = randomAddress(),
        x2y2TransferProxyErc721 = randomAddress(),
        x2y2TransferProxyErc1155 = randomAddress(),
        looksrareV2TransferManager = randomAddress()
    )
}