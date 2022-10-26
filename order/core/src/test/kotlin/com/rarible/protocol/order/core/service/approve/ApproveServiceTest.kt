package com.rarible.protocol.order.core.service.approve

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBoolean
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.core.MonoEthereum
import scalether.domain.Address

internal class ApproveServiceTest {
    private val exchangeContractAddresses = randomContractAddresses()
    private val transferProxyAddresses = randomProxyAddresses()
    private val approveRepository = mockk<ApprovalHistoryRepository>()
    private val ethereum = mockk<MonoEthereum>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val approveService = ApproveService(approveRepository, featureFlags, ethereum, exchangeContractAddresses, transferProxyAddresses)

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
    fun `should approve for x2y2 platforms`() = runBlocking<Unit> {
        testApproval(Platform.X2Y2, exchangeContractAddresses.x2y2V1)
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
            proxyWithNoEvent1 = transferProxyAddresses.looksrareTransferManagerERC1155,
            proxyWithNoEvent2 =  transferProxyAddresses.looksrareTransferManagerNonCompliantERC721
        )
    }

    @Test
    fun `should approve for looksrare non erc721`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareTransferManagerNonCompliantERC721,
            proxyWithNoEvent1 = transferProxyAddresses.looksrareTransferManagerERC1155,
            proxyWithNoEvent2 =  transferProxyAddresses.looksrareTransferManagerERC721
        )
    }

    @Test
    fun `should approve for looksrare erc1155`() = runBlocking<Unit> {
        `should approve for looksrare`(
            proxyWithEvent = transferProxyAddresses.looksrareTransferManagerERC1155,
            proxyWithNoEvent1 = transferProxyAddresses.looksrareTransferManagerERC721,
            proxyWithNoEvent2 =  transferProxyAddresses.looksrareTransferManagerNonCompliantERC721
        )
    }

    private fun `should approve for looksrare`(
        proxyWithEvent: Address,
        proxyWithNoEvent1: Address,
        proxyWithNoEvent2: Address,
    ) = runBlocking<Unit> {
        val maker = randomAddress()
        val token = randomAddress()
        val approved = randomBoolean()

        mockGetLogEvent(token, maker, proxyWithEvent, approved)
        mockGetLogEvent(token, maker, proxyWithNoEvent1, null)
        mockGetLogEvent(token, maker, proxyWithNoEvent2, null)
        val result = approveService.checkApprove(maker, token, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(approved)
        coVerify { approveRepository.lastApprovalLogEvent(token, maker, proxyWithEvent) }
        coVerify { approveRepository.lastApprovalLogEvent(token, maker, proxyWithNoEvent1) }
        coVerify { approveRepository.lastApprovalLogEvent(token, maker, proxyWithNoEvent2) }
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

    private fun mockGetLogEvent(collection: Address, maker: Address, proxy: Address, expectedApproval: Boolean?) {
        coEvery { approveRepository.lastApprovalLogEvent(collection, maker, proxy) } returns expectedApproval?.let { logEvent(it) }
    }

    private fun logEvent(approved: Boolean) = createLogEvent(randomApproveHistory(approved = approved))

    private fun randomContractAddresses() = OrderIndexerProperties.ExchangeContractAddresses(
        v1 = randomAddress(),
        v1Old = randomAddress(),
        v2 = randomAddress(),
        openSeaV1 = randomAddress(),
        openSeaV2 = randomAddress(),
        seaportV1 = randomAddress(),
        cryptoPunks = randomAddress(),
        zeroEx = randomAddress(),
        looksrareV1 = randomAddress(),
        x2y2V1 = randomAddress(),
    )
    private fun randomProxyAddresses() =OrderIndexerProperties.TransferProxyAddresses(
        transferProxy = randomAddress(),
        erc20TransferProxy = randomAddress(),
        erc721LazyTransferProxy = randomAddress(),
        erc1155LazyTransferProxy = randomAddress(),
        cryptoPunksTransferProxy = randomAddress(),
        seaportTransferProxy = randomAddress(),
        looksrareTransferManagerERC721 = randomAddress(),
        looksrareTransferManagerERC1155 = randomAddress(),
        looksrareTransferManagerNonCompliantERC721 = randomAddress()
    )
}