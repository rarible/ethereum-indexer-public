package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155LogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155RaribleLogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.CreateERC1155RaribleUserLogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.erc721.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import scalether.domain.Address

/**
 * Background job that re-indexes tokens (specified by `param`).
 */
class ReindexTokenTaskHandler(
    private val logListenService: LogListenService,
    private val tokenRegistrationService: TokenRegistrationService,
    private val ethereum: MonoEthereum,
    private val nftListenerProperties: NftListenerProperties,
) : TaskHandler<Long> {

    override val type: String
        get() = ReindexTokenTaskParams.ADMIN_REINDEX_TOKEN

    override suspend fun isAbleToRun(param: String): Boolean = true

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = ReindexTokenTaskParams.fromParamString(param)
        return fetchNormalBlockNumber()
            .flatMapMany { to -> reindexTokens(taskParam, from, to) }
            .map { it.first }
            .asFlow()
    }

    private fun reindexTokens(params: ReindexTokenTaskParams, from: Long?, end: Long): Flux<LongRange> {
        val collectionDescriptors = listOf(
            CollectionOwnershipTransferLogDescriptor(tokenRegistrationService, nftListenerProperties),

            // ERC-721
            CreateERC721LogDescriptor(),
            CreateERC721RaribleLogDescriptor(),
            CreateERC721RaribleUserLogDescriptor(),
            CreateERC721V4LogDescriptor(),

            // ERC-1155
            CreateERC1155LogDescriptor(),
            CreateERC1155RaribleLogDescriptor(),
            CreateERC1155RaribleUserLogDescriptor()
        ).map { it.overrideAddresses(params.tokens) }
        return Flux.mergeOrdered(
            compareBy { it.first },
            *collectionDescriptors.map {
                logListenService.reindexWithDescriptor(it, from ?: 1, end)
            }.toTypedArray()
        ).distinct()
    }

    private fun <T : EventData> LogEventDescriptor<T>.overrideAddresses(addresses: List<Address>): LogEventDescriptor<T> =
        object : LogEventDescriptor<T> by this {
            override fun getAddresses(): Mono<Collection<Address>> = addresses.toMono()
        }

    private fun fetchNormalBlockNumber(): Mono<Long> = ethereum.ethBlockNumber().map { it.toLong() }

}
