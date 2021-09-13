package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class TransferProxyService(
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses
) {
    init {
        logger.info("initializing addresses: $transferProxyAddresses")
    }

    fun getTransferProxy(assetType: AssetType): Address? {
        return when (assetType) {
            is Erc721AssetType -> transferProxyAddresses.transferProxy
            is Erc1155AssetType -> transferProxyAddresses.transferProxy
            is Erc20AssetType -> transferProxyAddresses.erc20TransferProxy
            is Erc721LazyAssetType -> transferProxyAddresses.erc721LazyTransferProxy
            is Erc1155LazyAssetType -> transferProxyAddresses.erc1155LazyTransferProxy
            is CryptoPunksAssetType -> transferProxyAddresses.cryptoPunksTransferProxy
            is GenerativeArtAssetType -> null
            is EthAssetType -> null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TransferProxyService::class.java)
    }
}
