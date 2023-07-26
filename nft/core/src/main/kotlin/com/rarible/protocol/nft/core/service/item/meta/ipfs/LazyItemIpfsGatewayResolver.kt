package com.rarible.protocol.nft.core.service.item.meta.ipfs

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.resolver.CustomIpfsGatewayResolver

/**
 * Class that intercepts IPFS gateway replacements for IPFS urls which have the same gateway as gateway used for
 * lazy NFT minting. We need to use it instead of internal gateways in order to make meta fetch faster.
 */
class LazyItemIpfsGatewayResolver(
    private val lazyItemGateway: String
) : CustomIpfsGatewayResolver {

    override fun getResourceUrl(
        ipfsUrl: IpfsUrl,
        gateway: String,
        replaceOriginalHost: Boolean
    ): String? {
        if (replaceOriginalHost && ipfsUrl.originalGateway == lazyItemGateway) {
            return ipfsUrl.original
        }
        return null
    }
}
