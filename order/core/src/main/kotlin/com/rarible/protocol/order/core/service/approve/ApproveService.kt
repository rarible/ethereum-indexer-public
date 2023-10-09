package com.rarible.protocol.order.core.service.approve

import com.rarible.contracts.erc721.IERC721
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.metric.ApprovalMetrics
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender

@Component
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class ApproveService(
    private val erc20Service: Erc20Service,
    private val approveRepository: ApprovalHistoryRepository,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    private val sender: ReadOnlyMonoTransactionSender,
    private val approvalMetrics: ApprovalMetrics,
    transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
) {
    private val raribleTransferProxy = transferProxyAddresses.transferProxy
    private val seaportTransferProxy = transferProxyAddresses.seaportTransferProxy
    private val x2y2TransferProxyErc721 = transferProxyAddresses.x2y2TransferProxyErc721
    private val x2y2TransferProxyErc1155 = transferProxyAddresses.x2y2TransferProxyErc1155
    private val cryptoPunksTransferProxy = transferProxyAddresses.cryptoPunksTransferProxy
    private val looksrareTransferProxyErc721 = transferProxyAddresses.looksrareTransferManagerERC721
    private val looksrareTransferProxyErc1155 = transferProxyAddresses.looksrareTransferManagerERC1155
    private val looksrareTransferProxyNonCompliantErc721 =
        transferProxyAddresses.looksrareTransferManagerNonCompliantERC721
    private val looksrareV2TransferManager = transferProxyAddresses.looksrareV2TransferManager

    private val platformByOperatorMap: Map<Address, Platform> = mapOf(
        raribleTransferProxy to Platform.RARIBLE,
        seaportTransferProxy to Platform.OPEN_SEA,
        x2y2TransferProxyErc721 to Platform.X2Y2,
        x2y2TransferProxyErc1155 to Platform.X2Y2,
        cryptoPunksTransferProxy to Platform.CRYPTO_PUNKS,
        looksrareTransferProxyErc721 to Platform.LOOKSRARE,
        looksrareTransferProxyErc1155 to Platform.LOOKSRARE,
        looksrareTransferProxyNonCompliantErc721 to Platform.LOOKSRARE,
        looksrareV2TransferManager to Platform.LOOKSRARE
    )
    private val operatorsByPlatformMap = platformByOperatorMap.entries.groupBy(
        { it.value }, { it.key }
    )
    val operators: Set<Address> = platformByOperatorMap.keys

    fun getPlatform(operator: Address): Platform? {
        return platformByOperatorMap[operator]
    }

    suspend fun checkOnChainApprove(maker: Address, make: AssetType, platform: Platform): Boolean {
        if (make.nft.not() || featureFlags.checkOnChainApprove.not()) return true
        val onChainApprove = checkOnChainApprove(maker, make.token, platform)
        approvalMetrics.onApprovalOnChainCheck(platform, onChainApprove)
        return if (featureFlags.applyOnChainApprove) onChainApprove else true
    }

    suspend fun checkApprove(
        owner: Address,
        collection: Address,
        platform: Platform,
        default: Boolean = false,
    ): Boolean {
        return checkPlatformApprove(platform) { hasApprove(owner, it, collection) } ?: run {
            logger.warn(
                "Can't find approval event for owner={}, collection={}, platform={}, use default={}",
                owner, collection, platform, default
            )
            approvalMetrics.onApprovalEventMiss(platform)
            default
        }
    }

    suspend fun checkOnChainApprove(
        owner: Address,
        collection: Address,
        platform: Platform
    ): Boolean {
        val contract = IERC721(collection, sender)
        return checkPlatformApprove(platform) {
            val result = contract.isApprovedForAll(owner, it).awaitFirst()
            logger.info(
                "Approval check result: owner={}, collection={}, operator={}, result={}", owner, collection, it, result
            )
            result
        } ?: error("Can't be null")
    }

    suspend fun checkOnChainErc20Allowance(maker: Address, make: Asset): Boolean {
        if (make.type.nft) {
            return true
        }
        val result = erc20Service.getOnChainTransferProxyAllowance(maker, make.type.token)
        return result >= make.value.value
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
                        .map { operator -> async { check(operator) } }
                        .awaitAll()
                        .filterNotNull()
                        .fold(null as? Boolean?) { initial, value -> (initial ?: value) || value }
                }
            }
            Platform.BLUR,
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

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ApproveService::class.java)
    }
}
