package com.rarible.protocol.nft.core.service.item.meta.ipfs

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.resolver.CustomIpfsGatewayResolver

class EthereumCustomIpfsGatewayResolver(
    private val resolvers: List<CustomIpfsGatewayResolver>
) : CustomIpfsGatewayResolver {

    override fun getResourceUrl(
        ipfsUrl: IpfsUrl,
        gateway: String,
        replaceOriginalHost: Boolean
    ): String? {
        for (resolver in resolvers) {
            resolver.getResourceUrl(ipfsUrl, gateway, replaceOriginalHost)?.let { return it }
        }
        return null
    }
}
