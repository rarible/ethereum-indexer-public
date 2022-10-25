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
    private val exchangeContractAddresses = randomContractAddresses()
    private val transferProxyAddresses = randomProxyAddresses()
    private val approveRepository = mockk<ApprovalHistoryRepository>()
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
        val proxy = transferProxyAddresses.transferProxy
        val expectedApproval = randomBoolean()
        mockGetLogEvent(nftAssetType.token, maker, proxy, expectedApproval)
        val result = approveService.hasNftCollectionApprove(maker, nftAssetType, Platform.RARIBLE)
        assertThat(result).isEqualTo(expectedApproval)
        coVerify { approveRepository.lastApprovalLogEvent(nftAssetType.token, maker, proxy) }
    }

    @ParameterizedTest
    @MethodSource("raribleLazy")
    fun `should approve lazy for rarible platform`(nftAssetType: NftCollectionAssetType) = runBlocking<Unit> {
        val result = approveService.hasNftCollectionApprove(randomAddress(), nftAssetType, Platform.RARIBLE)
        assertThat(result).isTrue
        coVerify(exactly = 0) { approveRepository.lastApprovalLogEvent(any(), any(), any()) }
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
        val maker = randomAddress()
        val proxy1: Address = transferProxyAddresses.looksrareTransferManagerERC721
        val proxy2: Address = transferProxyAddresses.looksrareTransferManagerNonCompliantERC721
        val asset: NftCollectionAssetType = randomErc721Type()

        mockGetLogEvent(asset.token, maker, proxy1, true)
        mockGetLogEvent(asset.token, maker, proxy2, false)
        val result = approveService.hasNftCollectionApprove(maker, asset, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(true)

        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy1) }
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy2) }
    }

    @Test
    fun `should approve for looksrare erc1155`() = runBlocking<Unit> {
        val maker = randomAddress()
        val proxy: Address = transferProxyAddresses.looksrareTransferManagerERC1155
        val asset: NftCollectionAssetType = randomErc1155Type()

        mockGetLogEvent(asset.token, maker, proxy, true)
        val result = approveService.hasNftCollectionApprove(maker, asset, Platform.LOOKSRARE)
        assertThat(result).isEqualTo(true)
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy) }
    }

    private suspend fun testApproval(platform: Platform, proxy: Address) {
        val maker = randomAddress()
        val expectedApproval = randomBoolean()
        val asset: NftCollectionAssetType = Erc721AssetType(randomAddress(), EthUInt256.of(randomInt()))
        mockGetLogEvent(asset.token, maker, proxy, expectedApproval)
        val result = approveService.hasNftCollectionApprove(maker, asset, platform)
        assertThat(result).isEqualTo(expectedApproval)
        coVerify { approveRepository.lastApprovalLogEvent(asset.token, maker, proxy) }
    }

    private fun mockProxyByPlatform(
        platform: Platform,
        commonProxy: Address = randomAddress(),
        lrErc721Proxy: Address = randomAddress(),
        lrErc115Proxy: Address = randomAddress(),
        lrErc721NoneProxy: Address = randomAddress(),
    ) {
        fun proxy(mockPlatform: Platform, proxy: Address): Address {
            return if (mockPlatform == platform) proxy else randomAddress()
        }
        every { transferProxyAddresses.transferProxy } returns proxy(Platform.RARIBLE, commonProxy)
        every { transferProxyAddresses.seaportTransferProxy } returns proxy(Platform.OPEN_SEA, commonProxy)
        every { transferProxyAddresses.cryptoPunksTransferProxy } returns proxy(Platform.CRYPTO_PUNKS, commonProxy)
        every { exchangeContractAddresses.x2y2V1 } returns proxy(Platform.X2Y2, commonProxy)
        every { transferProxyAddresses.looksrareTransferManagerERC721 } returns proxy(Platform.LOOKSRARE, lrErc721Proxy)
        every { transferProxyAddresses.looksrareTransferManagerERC1155 } returns proxy(Platform.LOOKSRARE, lrErc115Proxy)
        every { transferProxyAddresses.looksrareTransferManagerNonCompliantERC721 } returns proxy(Platform.LOOKSRARE, lrErc721NoneProxy)
    }

    private fun mockGetLogEvent(collection: Address, maker: Address, proxy: Address, expectedApproval: Boolean) {
        coEvery { approveRepository.lastApprovalLogEvent(collection, maker, proxy) } returns logEvent(expectedApproval)
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