package com.rarible.protocol.order.core.service.approve

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomErc1155Type
import com.rarible.protocol.order.core.data.randomErc721Type
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.NftCollectionAssetType
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.util.stream.Stream

internal class ApproveServiceTest {
    private val approveRepository = mockk<ApprovalHistoryRepository>()
    private val exchangeContractAddresses = mockk<OrderIndexerProperties.ExchangeContractAddresses>()
    private val transferProxyAddresses = mockk<OrderIndexerProperties.TransferProxyAddresses>()

    private val approveService = ApproveService(approveRepository, exchangeContractAddresses, transferProxyAddresses)

    private companion object {
        @JvmStatic
        fun rarible(): Stream<NftCollectionAssetType> = Stream.of(
            Erc1155AssetType(randomAddress(), EthUInt256.of(randomInt())),
            Erc721AssetType(randomAddress(), EthUInt256.of(randomInt())),
            CollectionAssetType(randomAddress()),
        )

        @JvmStatic
        fun raribleLazy(): Stream<NftCollectionAssetType> = Stream.of(
            Erc1155LazyAssetType(randomAddress(), EthUInt256.of(randomInt()), "", EthUInt256.ONE, emptyList(), emptyList(), emptyList()),
            Erc721LazyAssetType(randomAddress(), EthUInt256.of(randomInt()), "", emptyList(), emptyList(), emptyList()),
        )
    }

    @ParameterizedTest
    @MethodSource("rarible")
    fun `should approve for rarible platform`(nftAssetType: NftCollectionAssetType) = runBlocking<Unit> {
        val maker = randomAddress()
        val proxy = randomAddress()
        val expectedApproval = randomBoolean()

        mockGetProxyByPlatform(Platform.RARIBLE, proxy = proxy)
        mockGetLogEvent(nftAssetType.token, maker, proxy, expectedApproval)
        val result = approveService.hasNftCollectionApprove(maker, nftAssetType, Platform.RARIBLE)
        assertThat(result).isEqualTo(expectedApproval)

        verify { transferProxyAddresses.transferProxy }
        coVerify { approveRepository.lastApprovalLogEvent(nftAssetType.token, maker, proxy) }
    }

    @ParameterizedTest
    @MethodSource("raribleLazy")
    fun `should approve lazy for rarible platform`(nftAssetType: NftCollectionAssetType) = runBlocking<Unit> {
        val result = approveService.hasNftCollectionApprove(randomAddress(), nftAssetType, Platform.RARIBLE)
        assertThat(result).isTrue

        verify(exactly = 0) { transferProxyAddresses.transferProxy }
        coVerify(exactly = 0) { approveRepository.lastApprovalLogEvent(any(), any(), any()) }
    }

    @Test
    fun `should approve for x2y2 platforms`() = runBlocking<Unit> {
        testApproval(Platform.X2Y2)
        verify { exchangeContractAddresses.x2y2V1 }
    }

    @Test
    fun `should approve for seaport platforms`() = runBlocking<Unit> {
        testApproval(Platform.OPEN_SEA)
        verify { transferProxyAddresses.seaportTransferProxy }
    }

    @Test
    fun `should approve for crypto punks platforms`() = runBlocking<Unit> {
        testApproval(Platform.CRYPTO_PUNKS)
        verify { transferProxyAddresses.cryptoPunksTransferProxy }
    }

    @Test
    fun `should approve for looksrare erc721`() = runBlocking<Unit> {
        val maker = randomAddress()
        val proxy1: Address = randomAddress()
        val proxy2: Address = randomAddress()
        val asset: NftCollectionAssetType = randomErc721Type()

        every { transferProxyAddresses.looksrareTransferManagerERC721 } returns proxy1
        every { transferProxyAddresses.looksrareTransferManagerNonCompliantERC721 } returns proxy2

        mockGetLogEvent(asset.token, maker, proxy1, true)
        mockGetLogEvent(asset.token, maker, proxy2, false)
        val result = approveService.hasNftCollectionApprove(maker, asset, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(true)

        verify { transferProxyAddresses.looksrareTransferManagerERC721 }
        verify { transferProxyAddresses.looksrareTransferManagerNonCompliantERC721 }
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy1) }
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy2) }
    }

    @Test
    fun `should approve for looksrare erc1155`() = runBlocking<Unit> {
        val maker = randomAddress()
        val proxy: Address = randomAddress()
        val asset: NftCollectionAssetType = randomErc1155Type()

        every { transferProxyAddresses.looksrareTransferManagerERC1155 } returns proxy

        mockGetLogEvent(asset.token, maker, proxy, true)
        val result = approveService.hasNftCollectionApprove(maker, asset, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(true)

        verify { transferProxyAddresses.looksrareTransferManagerERC1155 }
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy) }
    }

    private suspend fun testApproval(platform: Platform) {
        val maker = randomAddress()
        val expectedApproval = randomBoolean()
        val proxy: Address = randomAddress()
        val asset: NftCollectionAssetType = Erc721AssetType(randomAddress(), EthUInt256.of(randomInt()))
        mockGetProxyByPlatform(platform, proxy = proxy)
        mockGetLogEvent(asset.token, maker, proxy, expectedApproval)
        val result = approveService.hasNftCollectionApprove(maker, asset, platform)
        assertThat(result).isEqualTo(expectedApproval)
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy) }
    }

    private fun mockGetProxyByPlatform(
        platform: Platform,
        proxy: Address = randomAddress(),
    ) {
        when (platform) {
            Platform.RARIBLE -> every { transferProxyAddresses.transferProxy } returns proxy
            Platform.OPEN_SEA -> every { transferProxyAddresses.seaportTransferProxy } returns proxy
            Platform.CRYPTO_PUNKS -> every { transferProxyAddresses.cryptoPunksTransferProxy } returns proxy
            Platform.X2Y2 -> every { exchangeContractAddresses.x2y2V1 } returns proxy
            Platform.SUDOSWAP -> { }
            Platform.LOOKSRARE -> { }
        }
    }

    private fun mockGetLogEvent(collection: Address, maker: Address, proxy: Address, expectedApproval: Boolean) {
        coEvery { approveRepository.lastApprovalLogEvent(collection, maker, proxy) } returns logEvent(expectedApproval)
    }

    private fun logEvent(approved: Boolean) = createLogEvent(randomApproveHistory(approved = approved))
}