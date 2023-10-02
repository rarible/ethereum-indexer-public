package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipUpdateService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

sealed class CompositeUpdateService(
    private val itemUpdateService: ItemUpdateService,
    private val ownershipUpdateService: OwnershipUpdateService,
    private val itemExStateRepository: ItemExStateRepository,
    private val properties: NftIndexerProperties,
    private val updateNotChanged: Boolean
) : EntityService<ItemId, CompositeEntity, CompositeEvent> {

    private val logger = LoggerFactory.getLogger(CompositeUpdateService::class.java)

    override suspend fun get(id: ItemId): CompositeEntity? {
        return null
    }

    override suspend fun getAll(ids: Collection<ItemId>): List<CompositeEntity> {
        return emptyList()
    }

    override suspend fun update(entity: CompositeEntity, event: CompositeEvent?): CompositeEntity {
        logger.info("Update composite, item=${entity.item?.id}, ownerships=${entity.ownerships.size}")

        return coroutineScope {
            val savedItem = entity.item?.let { async { updateItem(it) } }

            val savedOwnerships = entity.ownerships.values.chunked(properties.ownershipSaveBatch)
                .flatMap { ownerships ->
                    ownerships.map { async { updateOwnership(it) } }.awaitAll()
                }

            CompositeEntity(entity.id, savedItem?.await(), savedOwnerships.associateBy { it.owner }.toMutableMap())
        }
    }

    private suspend fun updateItem(reducedItem: Item): Item {
        val exState = itemExStateRepository.getById(reducedItem.id)
        val reducedItemWithExState = reducedItem.withState(exState)

        val existItem = itemUpdateService.get(reducedItemWithExState.id)
        val safeVersion = reducedItemWithExState.version ?: existItem?.version

        // Item doesn't exist, which means we have to update it anyway
        if (existItem == null || updateNotChanged) {
            return itemUpdateService.update(reducedItemWithExState.withVersion(safeVersion))
        }

        // Otherwise, we check if item has been changed, consider date/version as non-meaningful values
        val safeCompareItem = reducedItemWithExState.copy(
            version = existItem.version,
            date = existItem.date
        ).cleanEventTimeMarks()

        val safeExistItem = existItem.cleanEventTimeMarks()

        return if (safeCompareItem != safeExistItem) {
            itemUpdateService.update(reducedItemWithExState.withVersion(safeVersion))
        } else {
            logger.info(
                "Item [{}] hasn't been changed after the reduce, update skipped",
                reducedItemWithExState.id.decimalStringValue
            )
            existItem
        }
    }

    private suspend fun updateOwnership(reducedOwnership: Ownership): Ownership {
        val existOwnership = ownershipUpdateService.get(reducedOwnership.id)
        val safeVersion = reducedOwnership.version ?: existOwnership?.version

        if (existOwnership == null || updateNotChanged) {
            return ownershipUpdateService.update(reducedOwnership.withVersion(safeVersion))
        }
        val safeCompareOwnership = reducedOwnership.copy(
            version = existOwnership.version,
            lastUpdatedAt = existOwnership.lastUpdatedAt
        )
        return if (safeCompareOwnership != existOwnership) {
            ownershipUpdateService.update(reducedOwnership.withVersion(safeVersion))
        } else {
            logger.info(
                "Ownership [{}] hasn't been changed after the reduce, update skipped",
                reducedOwnership.id.decimalStringValue
            )
            existOwnership
        }
    }
}

@Component
@Qualifier("SilentCompositeUpdateService")
class SilentCompositeUpdateService(
    itemUpdateService: ItemUpdateService,
    ownershipUpdateService: OwnershipUpdateService,
    itemExStateRepository: ItemExStateRepository,
    properties: NftIndexerProperties
) : CompositeUpdateService(itemUpdateService, ownershipUpdateService, itemExStateRepository, properties, false)

@Component
@Qualifier("VerboseCompositeUpdateService")
class VerboseCompositeUpdateService(
    itemUpdateService: ItemUpdateService,
    ownershipUpdateService: OwnershipUpdateService,
    itemExStateRepository: ItemExStateRepository,
    properties: NftIndexerProperties
) : CompositeUpdateService(itemUpdateService, ownershipUpdateService, itemExStateRepository, properties, true)
