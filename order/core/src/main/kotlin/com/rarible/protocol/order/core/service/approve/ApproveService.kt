package com.rarible.protocol.order.core.service.approve

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.NftCollectionAssetType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ApproveService(
    private val approveRepository: ApprovalHistoryRepository,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
) {
    suspend fun hasNftCollectionApprove(
        maker: Address,
        nftAssetType: NftCollectionAssetType,
        platform: Platform
    ): Boolean {
        val proxy = when (platform) {
            Platform.OPEN_SEA -> transferProxyAddresses.seaportTransferProxy
            Platform.X2Y2 -> exchangeContractAddresses.x2y2V1
            Platform.CRYPTO_PUNKS -> transferProxyAddresses.cryptoPunksTransferProxy
            Platform.RARIBLE -> return handleRarible(maker, nftAssetType)
            Platform.LOOKSRARE -> return handleLooksrare(maker, nftAssetType)
            Platform.SUDOSWAP -> return true
        }
        return hasApprove(proxy, maker, nftAssetType.token)
    }

    private suspend fun handleRarible(
        maker: Address,
        nftAssetType: NftCollectionAssetType,
    ): Boolean {
        return when (nftAssetType) {
            is Erc1155AssetType,
            is Erc721AssetType,
            is CollectionAssetType -> {
                hasApprove(transferProxyAddresses.transferProxy, maker, nftAssetType.token)
            }
            is Erc1155LazyAssetType,
            is Erc721LazyAssetType -> true
            is CryptoPunksAssetType -> throw getUnsupportedAssetException(nftAssetType, Platform.RARIBLE)
        }
    }

    private suspend fun handleLooksrare(
        maker: Address,
        nftAssetType: NftCollectionAssetType,
    ): Boolean {
        return when (nftAssetType) {
            is Erc721AssetType -> {
                return coroutineScope {
                    val erc721Approve = async {
                        hasApprove(transferProxyAddresses.looksrareTransferManagerERC721, maker, nftAssetType.token)
                    }
                    val nonCompliantErc721Approve = async {
                        hasApprove(transferProxyAddresses.looksrareTransferManagerNonCompliantERC721, maker, nftAssetType.token)
                    }
                    erc721Approve.await() || nonCompliantErc721Approve.await()
                }
            }
            is Erc1155AssetType -> {
                hasApprove(transferProxyAddresses.looksrareTransferManagerERC1155, maker, nftAssetType.token)
            }
            is CollectionAssetType,
            is CryptoPunksAssetType,
            is Erc1155LazyAssetType,
            is Erc721LazyAssetType -> throw getUnsupportedAssetException(nftAssetType, Platform.LOOKSRARE)
        }
    }

    fun getUnsupportedAssetException(nftAssetType: NftCollectionAssetType, platform: Platform): Throwable {
        return UnsupportedOperationException("Unsupported assert type $nftAssetType for platform $platform")
    }

    private suspend fun hasApprove(proxy: Address, maker: Address, collection: Address): Boolean {
        return approveRepository.lastApprovalLogEvent(
            collection = collection,
            owner = maker,
            operator = proxy
        )?.let { (it.data as ApprovalHistory).approved } ?: true
    }
}