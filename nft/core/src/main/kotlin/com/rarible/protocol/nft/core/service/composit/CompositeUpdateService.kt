package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.apm.withTransaction
import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
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

    private val updateIfNothingChanged = true

    override suspend fun get(id: ItemId): CompositeEntity? {
        return null
    }

    override suspend fun update(entity: CompositeEntity): CompositeEntity {
        logger.info("Update composite, item=${entity.item?.id}, ownerships=${entity.ownerships.size}")

        return withTransaction(
            "updateCompositeEntity", labels = listOf("itemId" to (entity.item?.id?.toString() ?: ""))
        ) {
            coroutineScope {
                val savedItem = entity.item?.let { async { updateItem(it) } }

                val savedOwnerships = entity.ownerships.values.chunked(properties.ownershipSaveBatch)
                    .flatMap { ownerships ->
                        ownerships.map { async { updateOwnership(it) } }.awaitAll()
                    }

                CompositeEntity(entity.id, savedItem?.await(), savedOwnerships.associateBy { it.owner }.toMutableMap())
            }
        }
    }

    private suspend fun updateItem(reducedItem: Item): Item {
        val existItem = itemUpdateService.get(reducedItem.id)
        val safeVersion = reducedItem.version ?: existItem?.version

        // Item doesn't exist, which means we have to update it anyway
        if (existItem == null || updateIfNothingChanged) {
            return itemUpdateService.update(reducedItem.withVersion(safeVersion))
        }

        // Otherwise, we check if item has been changed, consider date/version as non-meaningful values
        val safeCompareItem = reducedItem.copy(version = existItem.version, date = existItem.date)

        return if (safeCompareItem != existItem) {
            itemUpdateService.update(reducedItem.withVersion(safeVersion))
        } else {
            logger.info(
                "Item [{}] hasn't been changed after the reduce, update skipped",
                reducedItem.id.decimalStringValue
            )
            existItem
        }
    }

    private suspend fun updateOwnership(reducedOwnership: Ownership): Ownership {
        val existOwnership = ownershipUpdateService.get(reducedOwnership.id)
        val safeVersion = reducedOwnership.version ?: existOwnership?.version

        if (existOwnership == null || updateIfNothingChanged) {
            return ownershipUpdateService.update(reducedOwnership.withVersion(safeVersion))
        }

        val safeCompareOwnership = reducedOwnership.copy(
            version = existOwnership.version,
            lastUpdatedAt = existOwnership.date
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
