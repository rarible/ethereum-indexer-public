package com.rarible.protocol.order.core.service.approve

import com.rarible.contracts.erc721.IERC721
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.lang.IllegalArgumentException

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ApproveService(
    private val approveRepository: ApprovalHistoryRepository,
    ethereum: MonoEthereum,
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
) {
    private val sender = ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())
    private val raribleTransferProxy = transferProxyAddresses.transferProxy
    private val seaportTransferProxy = transferProxyAddresses.seaportTransferProxy
    private val x2y2TransferProxy = exchangeContractAddresses.x2y2V1
    private val cryptoPunksTransferProxy = transferProxyAddresses.cryptoPunksTransferProxy
    private val looksrareTransferProxyErc721 = transferProxyAddresses.looksrareTransferManagerERC721
    private val looksrareTransferProxyErc1155 = transferProxyAddresses.looksrareTransferManagerERC1155
    private val looksrareTransferProxyNonCompliantErc721 = transferProxyAddresses.looksrareTransferManagerNonCompliantERC721

    private val platformByOperatorMap: Map<Address, Platform> = mapOf(
        raribleTransferProxy to Platform.RARIBLE,
        seaportTransferProxy to Platform.OPEN_SEA,
        x2y2TransferProxy to Platform.X2Y2,
        cryptoPunksTransferProxy to Platform.CRYPTO_PUNKS,
        looksrareTransferProxyErc721 to Platform.LOOKSRARE,
        looksrareTransferProxyErc1155 to Platform.LOOKSRARE,
        looksrareTransferProxyNonCompliantErc721 to Platform.LOOKSRARE
    )
    private val operatorsByPlatformMap = platformByOperatorMap.entries.groupBy(
        { it.value }, { it.key }
    )
    val operators: Set<Address> = platformByOperatorMap.keys

    fun getPlatform(operator: Address): Platform? {
        return platformByOperatorMap[operator]
    }

    suspend fun checkOnChainApprove(
        owner: Address,
        collection: Address,
        platform: Platform
    ): Boolean {
        val contract = IERC721(collection, sender)
        return checkPlatformApprove(platform) {  contract.isApprovedForAll(owner, it).awaitFirst() } ?: error("Can't be null")
    }

    suspend fun checkApprove(
        owner: Address,
        collection: Address,
        platform: Platform,
        default: Boolean = false,
    ): Boolean {
        return checkPlatformApprove(platform) { hasApprove(owner, it, collection)  } ?: default
    }

    private suspend fun checkPlatformApprove(
        platform: Platform,
        check: suspend (Address) -> Boolean?
    ): Boolean? {
        return when (platform) {
            Platform.RARIBLE,
            Platform.OPEN_SEA,
            Platform.CRYPTO_PUNKS,
            Platform.LOOKSRARE,
            Platform.X2Y2 -> {
                val operators = operatorsByPlatformMap[platform]
                    ?: throw IllegalArgumentException("Can't find operators for platform $platform")
                coroutineScope {
                    operators
                        .map { operator -> async { check(operator) }  }
                        .awaitAll()
                        .filterNotNull()
                        .fold(null as? Boolean?) { initial, value -> (initial ?: value) || value }
                }
            }
            Platform.SUDOSWAP -> true
        }
    }

    private suspend fun hasApprove(owner: Address, proxy: Address, collection: Address): Boolean? {
        return approveRepository.lastApprovalLogEvent(
            collection = collection,
            owner = owner,
            operator = proxy
        )?.let {
            (it.data as ApprovalHistory).approved
        }
    }
}