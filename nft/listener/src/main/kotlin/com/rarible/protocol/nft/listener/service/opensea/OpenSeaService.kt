package com.rarible.protocol.nft.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.v1.AssetsRequest
import com.rarible.opensea.client.model.v1.OpenSeaAssets
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class OpenSeaService(
    private val openSeaClient: OpenSeaClient,
) {
    suspend fun getOpenSeaAssets(contract: Address, cursor: String?): OpenSeaAssets {
        val request = AssetsRequest(
            contracts = listOf(contract),
            tokenIds = emptyList(),
            limit = 50,
            cursor = cursor
        )
        return openSeaClient.getAssets(request).ensureSuccess()
    }
}
