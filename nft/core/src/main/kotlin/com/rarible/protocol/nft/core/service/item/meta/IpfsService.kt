package com.rarible.protocol.nft.core.service.item.meta

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class IpfsService(
    @Value("\${api.ipfs-url}") private val ipfsUrl: String
) {
    fun resolveIpfsUrl(uri: String): String =
        if (uri.contains("/ipfs/")) {
            "ipfs:/${uri.substring(uri.lastIndexOf("/ipfs/"))}"
        } else {
            uri
        }

    fun resolveRealUrl(uri: String): String {
        val ipfsProtocolUri = resolveIpfsUrl(uri)
        return when {
            ipfsProtocolUri.startsWith("http") -> ipfsProtocolUri
            ipfsProtocolUri.startsWith("ipfs:///ipfs/") -> "$ipfsUrl/ipfs/${ipfsProtocolUri.substring("ipfs:///ipfs/".length)}"
            ipfsProtocolUri.startsWith("ipfs://ipfs/") -> "$ipfsUrl/ipfs/${ipfsProtocolUri.substring("ipfs://ipfs/".length)}"
            ipfsProtocolUri.startsWith("Qm") -> "$ipfsUrl/ipfs/$ipfsProtocolUri"
            else -> "$ipfsUrl$ipfsProtocolUri"
        }
    }

    companion object {
        const val IPFS_NEW_URL = "https://ipfs.rarible.com"
    }
}
