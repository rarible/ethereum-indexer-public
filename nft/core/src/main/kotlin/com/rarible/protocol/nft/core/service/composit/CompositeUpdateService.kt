package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.apm.withTransaction
import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipUpdateService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CompositeUpdateService(
    private val itemUpdateService: ItemUpdateService,
    private val ownershipUpdateService: OwnershipUpdateService,
    private val properties: NftIndexerProperties
) : EntityService<ItemId, CompositeEntity> {
    private val logger = LoggerFactory.getLogger(CompositeUpdateService::class.java)

    override suspend fun get(id: ItemId): CompositeEntity? {
        return null
    }

    override suspend fun update(entity: CompositeEntity): CompositeEntity {
        logger.info("Update composite, item=${entity.item?.id}, ownerships=${entity.ownerships.size}")

        return withTransaction("updateCompositeEntity", labels = listOf("itemId" to (entity.item?.id?.toString() ?: ""))) {
            coroutineScope {

                val savedItem = entity.item?.let {
                    async {
                        val version = it.version ?: itemUpdateService.get(it.id)?.version
                        itemUpdateService.update(it.withVersion(version))
                    }
                }
                val savedOwnerships = entity.ownerships.values.chunked(properties.ownershipSaveBatch)
                    .flatMap { ownerships ->
                        ownerships.map {
                            async {
                                val version = it.version ?: ownershipUpdateService.get(it.id)?.version
                                ownershipUpdateService.update(it.withVersion(version))
                            }
                        }.awaitAll()
                    }
                CompositeEntity(entity.id, savedItem?.await(), savedOwnerships.associateBy { it.owner }.toMutableMap())
            }
        }
    }
}
