package com.rarible.protocol.order.core.service.asset

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.order.core.data.randomErc1155LazyAssetType
import com.rarible.protocol.order.core.data.randomErc721LazyAssetType
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.service.balance.BalanceControllerApiService
import com.rarible.protocol.order.core.service.balance.EthBalanceService
import com.rarible.protocol.order.core.service.nft.NftOwnershipApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Instant

internal class AssetBalanceProviderImplTest {
    private val erc20BalanceApi = mockk<BalanceControllerApiService>()
    private val nftOwnershipApi = mockk<NftOwnershipApiService>()
    private val ethBalanceService =  mockk<EthBalanceService>()

    private val assetBalanceProviderImpl = AssetBalanceProviderImpl(
        erc20BalanceApi,
        nftOwnershipApi,
        ethBalanceService
    )

    @Test
    fun `should get value for erc721 lazy assert`() = runBlocking<Unit> {
        val assert = randomErc721LazyAssetType()
        val owner = randomAddress()

        coEvery { nftOwnershipApi.getOwnershipById("${assert.token}:${assert.tokenId}:$owner") } returns NftOwnershipDto(
            contract = randomAddress(),
            creators = emptyList(),
            date = Instant.now(),
            id = "",
            lazyValue = EthUInt256.ZERO.value,
            owner = randomAddress(),
            pending = emptyList(),
            tokenId = BigInteger.ZERO,
            value = BigInteger.TEN
        )

        val balance = assetBalanceProviderImpl.getAssetStock(owner, Asset(assert, EthUInt256.ONE))
        assertThat(balance!!.value).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should get value for erc1155 lazy assert`() = runBlocking<Unit> {
        val assert = randomErc1155LazyAssetType()
        val owner = randomAddress()

        coEvery { nftOwnershipApi.getOwnershipById("${assert.token}:${assert.tokenId}:$owner") } returns NftOwnershipDto(
            contract = randomAddress(),
            creators = emptyList(),
            date = Instant.now(),
            id = "",
            lazyValue = EthUInt256.ZERO.value,
            owner = randomAddress(),
            pending = emptyList(),
            tokenId = BigInteger.ZERO,
            value = BigInteger.TEN
        )

        val balance = assetBalanceProviderImpl.getAssetStock(owner, Asset(assert, EthUInt256.ONE))
        assertThat(balance!!.value).isEqualTo(EthUInt256.TEN)
    }
}
