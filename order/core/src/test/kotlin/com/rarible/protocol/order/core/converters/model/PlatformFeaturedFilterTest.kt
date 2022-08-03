package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlatformFeaturedFilterTest {

    @Test
    fun `show only rarible, opensea disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = false,
                showOpenSeaOrdersWithOtherPlatforms = false
            )
        )

        val raribleOnly = filter.filter(null)
        assertThat(raribleOnly).isEqualTo(listOf(PlatformDto.RARIBLE))

        val onlyOpenSea = filter.filter(PlatformDto.OPEN_SEA)
        assertThat(onlyOpenSea).isEqualTo(listOf(PlatformDto.OPEN_SEA))
    }

    @Test
    fun `show only rarible, opensea enabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = false,
                showOpenSeaOrdersWithOtherPlatforms = true
            )
        )

        val raribleOnly = filter.filter(null)
        assertThat(raribleOnly).isEqualTo(listOf(PlatformDto.RARIBLE))

        val onlyOpenSea = filter.filter(PlatformDto.OPEN_SEA)
        assertThat(onlyOpenSea).isEqualTo(listOf(PlatformDto.OPEN_SEA))
    }

    @Test
    fun `show all, opensea disabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = false
            )
        )

        val allByDefaultExceptOpenSea = filter.filter(null)
        assertThat(allByDefaultExceptOpenSea).isEqualTo(listOf(PlatformDto.RARIBLE, PlatformDto.CRYPTO_PUNKS, PlatformDto.X2Y2, PlatformDto.LOOKSRARE))
    }

    @Test
    fun `show all, opensea enabled`() {
        val filter = PlatformFeaturedFilter(
            OrderIndexerProperties.FeatureFlags(
                showAllOrdersByDefault = true,
                showOpenSeaOrdersWithOtherPlatforms = true
            )
        )

        val allWithOpenSea = filter.filter(null)
        assertThat(allWithOpenSea).isEqualTo(
            emptyList<PlatformDto>()
        )
    }

}
