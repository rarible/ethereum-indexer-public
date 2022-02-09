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
        throw UnsupportedOperationException("Must not be called")
    }

    override suspend fun update(entity: CompositeEntity): CompositeEntity {
        logger.info("Update composite, item=${entity.item?.id}, ownerships=${entity.ownerships.size}")

        return withTransaction("updateCompositeEntity", labels = listOf("itemId" to (entity.item?.id?.toString() ?: ""))) {
            coroutineScope {

                val savedItem = entity.item?.let {
                    async {
                        itemUpdateService.update(it)
                    }
                }
                val savedOwnerships = entity.ownerships.values.chunked(properties.ownershipSaveBatch)
                    .flatMap { ownerships ->
                        ownerships.map {
                            async {
                                ownershipUpdateService.update(it)
                            }
                        }.awaitAll()
                    }
                CompositeEntity(entity.id, savedItem?.await(), savedOwnerships.associateBy { it.owner }.toMutableMap())
            }
        }
    }
}
