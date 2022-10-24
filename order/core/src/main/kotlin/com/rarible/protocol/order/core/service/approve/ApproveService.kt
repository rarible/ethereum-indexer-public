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
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
) {
    private val raribleTransferProxy = transferProxyAddresses.transferProxy
    private val seaportTransferProxy = transferProxyAddresses.seaportTransferProxy
    private val x2y2TransferProxy = exchangeContractAddresses.x2y2V1
    private val cryptoPunksTransferProxy = transferProxyAddresses.cryptoPunksTransferProxy
    private val looksrareTransferProxyErc721 = transferProxyAddresses.looksrareTransferManagerERC721
    private val looksrareTransferProxyErc1155 = transferProxyAddresses.looksrareTransferManagerERC1155
    private val looksrareTransferProxyNonCompliantErc721 = transferProxyAddresses.looksrareTransferManagerNonCompliantERC721

    private val platformOperators: Map<Address, Platform> = mapOf(
        raribleTransferProxy to Platform.RARIBLE,
        seaportTransferProxy to Platform.OPEN_SEA,
        x2y2TransferProxy to Platform.X2Y2,
        cryptoPunksTransferProxy to Platform.CRYPTO_PUNKS,
        looksrareTransferProxyErc721 to Platform.LOOKSRARE,
        looksrareTransferProxyErc1155 to Platform.LOOKSRARE,
        looksrareTransferProxyNonCompliantErc721 to Platform.LOOKSRARE
    )

    val operators: Set<Address> = platformOperators.keys

    fun getPlatform(operator: Address): Platform? {
        return platformOperators[operator]
    }

    suspend fun hasNftCollectionApprove(
        maker: Address,
        nftAssetType: NftCollectionAssetType,
        platform: Platform
    ): Boolean {
        val proxy = when (platform) {
            Platform.OPEN_SEA -> seaportTransferProxy
            Platform.X2Y2 -> x2y2TransferProxy
            Platform.CRYPTO_PUNKS -> cryptoPunksTransferProxy
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
                hasApprove(raribleTransferProxy, maker, nftAssetType.token)
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
                        hasApprove(looksrareTransferProxyErc721, maker, nftAssetType.token)
                    }
                    val nonCompliantErc721Approve = async {
                        hasApprove(looksrareTransferProxyNonCompliantErc721, maker, nftAssetType.token)
                    }
                    erc721Approve.await() || nonCompliantErc721Approve.await()
                }
            }
            is Erc1155AssetType -> {
                hasApprove(looksrareTransferProxyErc1155, maker, nftAssetType.token)
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
        )?.let {
            (it.data as ApprovalHistory).approved
        } ?: true
    }
}