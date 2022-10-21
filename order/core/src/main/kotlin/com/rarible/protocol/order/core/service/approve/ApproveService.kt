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
        suspend fun hasMakerApprove(proxy: Address): Boolean {
            return hasApprove(proxy = proxy, maker = maker, collection = nftAssetType.token)
        }
        fun getUnsupportedAssetException(): Throwable {
            return UnsupportedOperationException("Unsupported assert type $nftAssetType for platform $platform")
        }
        val proxy = when (platform) {
            Platform.RARIBLE -> when (nftAssetType) {
                is Erc1155AssetType,
                is Erc721AssetType,
                is CollectionAssetType -> transferProxyAddresses.transferProxy
                is Erc1155LazyAssetType,
                is Erc721LazyAssetType -> return true
                is CryptoPunksAssetType -> throw getUnsupportedAssetException()
            }
            Platform.LOOKSRARE -> when (nftAssetType) {
                is Erc721AssetType -> {
                    return coroutineScope {
                        val erc721Approve = async {
                            hasMakerApprove(transferProxyAddresses.looksrareTransferManagerERC721)
                        }
                        val nonCompliantErc721Approve = async {
                            hasMakerApprove(transferProxyAddresses.looksrareTransferManagerNonCompliantERC721)
                        }
                        erc721Approve.await() || nonCompliantErc721Approve.await()
                    }
                }
                is Erc1155AssetType -> transferProxyAddresses.looksrareTransferManagerERC1155
                is CollectionAssetType,
                is CryptoPunksAssetType,
                is Erc1155LazyAssetType,
                is Erc721LazyAssetType -> throw getUnsupportedAssetException()
            }
            Platform.X2Y2 -> exchangeContractAddresses.x2y2V1
            Platform.OPEN_SEA -> transferProxyAddresses.seaportTransferProxy
            Platform.CRYPTO_PUNKS -> transferProxyAddresses.cryptoPunksTransferProxy
            Platform.SUDOSWAP -> return true
        }
        return hasMakerApprove(proxy)
    }

    private suspend fun hasApprove(proxy: Address, maker: Address, collection: Address): Boolean {
        return approveRepository.lastApprovalLogEvent(
            collection = collection,
            owner = maker,
            operator = proxy
        )?.let { (it.data as ApprovalHistory).approved } ?: true //TODO: Need to make false
    }
}