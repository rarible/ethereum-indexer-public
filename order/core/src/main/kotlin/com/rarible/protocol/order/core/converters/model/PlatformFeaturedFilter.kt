package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties

class PlatformFeaturedFilter(
    private val featureFlags: OrderIndexerProperties.FeatureFlags
) {

    private val allPlatforms = PlatformDto.values().toList()

    fun filter(platform: PlatformDto?): List<PlatformDto> {
        // Until specific FF is on, we are showing only RARIBLE orders
        val defaultPlatform = platform
            ?: if (featureFlags.showAllOrdersByDefault) null else PlatformDto.RARIBLE

        // Until specific FF is on, we are excluding OPEN_SEA platform from results
        if (defaultPlatform == null) {
            val all = ArrayList(allPlatforms)
            if (!featureFlags.showOpenSeaOrdersWithOtherPlatforms) {
                all.remove(PlatformDto.OPEN_SEA)
            }
            return all
        }

        return listOfNotNull(defaultPlatform)
    }
}
