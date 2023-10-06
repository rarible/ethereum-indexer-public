package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.listener.log.LogListenService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ReindexCryptoPunksTaskParam
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkAssignLogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkBoughtLogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.crypto.punks.CryptoPunkTransferLogDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum

// TODO should be refactored to be compatible with V2 Scanner, or removed (PT-3910)
class ReindexCryptoPunksTaskHandler(
    private val logListenService: LogListenService,
    private val tokenService: TokenService,
    private val ethereum: MonoEthereum,
    private val properties: NftIndexerProperties,
) : TaskHandler<Long> {

    override val type: String
        get() = ReindexCryptoPunksTaskParam.ADMIN_REINDEX_CRYPTO_PUNKS

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: Long?, param: String): Flow<Long> {
        val taskParam = ReindexCryptoPunksTaskParam.fromParamString(param)

        return fetchNormalBlockNumber()
            .flatMapMany { to -> reindex(taskParam, from, to) }
            .map { it.first }
            .asFlow()
    }

    private fun reindex(params: ReindexCryptoPunksTaskParam, from: Long?, end: Long): Flux<LongRange> {
        val descriptor = when (params.event) {
            ReindexCryptoPunksTaskParam.PunkEvent.TRANSFER ->
                CryptoPunkTransferLogDescriptor(tokenService, properties)
            ReindexCryptoPunksTaskParam.PunkEvent.ASSIGN ->
                CryptoPunkAssignLogDescriptor(tokenService, properties)
            ReindexCryptoPunksTaskParam.PunkEvent.BOUGHT ->
                CryptoPunkBoughtLogDescriptor(tokenService, properties, ethereum)
        }
        return logListenService.reindexWithDescriptor(descriptor, from ?: params.from, end)
    }

    private fun fetchNormalBlockNumber(): Mono<Long> {
        return ethereum.ethBlockNumber().map { it.toLong() }
    }
}
