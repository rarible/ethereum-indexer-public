package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlatformFeaturedFilterTest {

    @Test
    fun `show only rarible, opensea, x2y2, looksrare disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = false,
                showOpenSeaOrdersWithOtherPlatforms = false,
                showX2Y2OrdersWithOtherPlatforms = false,
                showLooksrareOrdersWithOtherPlatforms = false
            )
        )
        val raribleOnly = filter.filter(null)
        assertThat(raribleOnly).isEqualTo(listOf(PlatformDto.RARIBLE))

        val onlyOpenSea = filter.filter(PlatformDto.OPEN_SEA)
        assertThat(onlyOpenSea).isEqualTo(listOf(PlatformDto.OPEN_SEA))

        val onlyX2Y2 = filter.filter(PlatformDto.X2Y2)
        assertThat(onlyX2Y2).isEqualTo(listOf(PlatformDto.X2Y2))

        val onlyLooksrare = filter.filter(PlatformDto.LOOKSRARE)
        assertThat(onlyLooksrare).isEqualTo(listOf(PlatformDto.LOOKSRARE))
    }

    @Test
    fun `show only rarible, opensea, x2y2, looksrare enabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = false,
                showOpenSeaOrdersWithOtherPlatforms = true,
                showX2Y2OrdersWithOtherPlatforms = true,
                showLooksrareOrdersWithOtherPlatforms = true
            )
        )
        val raribleOnly = filter.filter(null)
        assertThat(raribleOnly).isEqualTo(listOf(PlatformDto.RARIBLE))

        val onlyOpenSea = filter.filter(PlatformDto.OPEN_SEA)
        assertThat(onlyOpenSea).isEqualTo(listOf(PlatformDto.OPEN_SEA))

        val onlyX2Y2 = filter.filter(PlatformDto.X2Y2)
        assertThat(onlyX2Y2).isEqualTo(listOf(PlatformDto.X2Y2))

        val onlyLooksrare = filter.filter(PlatformDto.LOOKSRARE)
        assertThat(onlyLooksrare).isEqualTo(listOf(PlatformDto.LOOKSRARE))
    }

    @Test
    fun `show all, opensea disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = false,
                showX2Y2OrdersWithOtherPlatforms = true,
                showLooksrareOrdersWithOtherPlatforms = true
            )
        )

        val allByDefaultExceptOpenSea = filter.filter(null)
        assertThat(allByDefaultExceptOpenSea).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.CRYPTO_PUNKS, PlatformDto.X2Y2, PlatformDto.LOOKSRARE, PlatformDto.SUDOSWAP))
    }

    @Test
    fun `show all, x2y2 disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = true,
                showLooksrareOrdersWithOtherPlatforms = true,
                showX2Y2OrdersWithOtherPlatforms = false
            )
        )

        val allByDefaultExceptOpenSea = filter.filter(null)
        assertThat(allByDefaultExceptOpenSea).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA, PlatformDto.CRYPTO_PUNKS, PlatformDto.LOOKSRARE, PlatformDto.SUDOSWAP))
    }

    @Test
    fun `show all, looksrare disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = true,
                showX2Y2OrdersWithOtherPlatforms = true,
                showLooksrareOrdersWithOtherPlatforms = false
            )
        )

        val allByDefaultExceptOpenSea = filter.filter(null)
        assertThat(allByDefaultExceptOpenSea).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA, PlatformDto.CRYPTO_PUNKS, PlatformDto.X2Y2, PlatformDto.SUDOSWAP))
    }

    @Test
    fun `show all, opensea, x2y2, looksrare enabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = true,
                showX2Y2OrdersWithOtherPlatforms = true,
                showLooksrareOrdersWithOtherPlatforms = true
            )
        )

        val allWithOpenSea = filter.filter(null)
        assertThat(allWithOpenSea).isEqualTo(
            emptyList<PlatformDto>()
        )
    }

    @Test
    fun `show all, opensea, x2y2, looksrare disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = false,
                showX2Y2OrdersWithOtherPlatforms = false,
                showLooksrareOrdersWithOtherPlatforms = false
            )
        )
        val allWithRarible = filter.filter(null)
        assertThat(allWithRarible).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.CRYPTO_PUNKS, PlatformDto.SUDOSWAP))
    }

    @Test
    fun `show all, x2y2, looksrare disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = true,
                showX2Y2OrdersWithOtherPlatforms = false,
                showLooksrareOrdersWithOtherPlatforms = false
            )
        )
        val allWithRarible = filter.filter(null)
        assertThat(allWithRarible).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA, PlatformDto.CRYPTO_PUNKS, PlatformDto.SUDOSWAP))
    }
}
