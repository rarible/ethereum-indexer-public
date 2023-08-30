package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.slf4j.LoggerFactory

class ConsistencyCorrectorEntityService(
    private val delegate: EntityService<ItemId, CompositeEntity, CompositeEvent>,
    private val reducer: CompositeReducer,
    private val itemEventConverter: ItemEventConverter,
) : EntityService<ItemId, CompositeEntity, CompositeEvent> {

    private val logger = LoggerFactory.getLogger(ConsistencyCorrectorEntityService::class.java)

    override suspend fun get(id: ItemId): CompositeEntity? {
        return null
    }

    override suspend fun getAll(ids: Collection<ItemId>): List<CompositeEntity> {
        return emptyList()
    }

    override suspend fun update(entity: CompositeEntity, event: CompositeEvent?): CompositeEntity {
        val itemId = entity.id
        val itemSupply = entity.item?.supply?.value
        val ownershipsSupply = entity.ownerships.values.sumOf { it.value.value }
        return if (itemSupply == ownershipsSupply) {
            entity
        } else {
            logger.info("Detected item $itemId inconsistency: itemSupply=$itemSupply, ownershipsSupply=$ownershipsSupply")
            val fixedItem = if (itemSupply == null) {
                logger.info("No mint detected for item ${entity.id}")
                reduceNullItem(entity)
            } else {
                entity.item
            }.copy(supply = EthUInt256.of(ownershipsSupply))

            CompositeEntity(
                id = entity.id,
                item = fixedItem,
                ownerships = entity.ownerships,
                firstEvent = entity.firstEvent
            )
        }.let { delegate.update(it) }
    }

    private suspend fun reduceNullItem(entity: CompositeEntity): Item {
        val itemId = entity.id
        val firstEvent = entity.firstEvent ?: throw IllegalStateException("Can't detect first event for item $itemId")
        if (firstEvent.itemEvent != null) throw IllegalStateException("Empty item $itemId with not empty item event $entity")

        val ownershipEvents = firstEvent.ownershipEvents
            .filterIsInstance<OwnershipEvent.TransferToEvent>()
            .firstOrNull()
            ?: throw IllegalStateException("Can't find TransferToEvent event $itemId ownership events")

        val mintEvent = itemEventConverter.convertToMintEvent(ownershipEvents)
        return reducer.reduce(CompositeEntity(entity.id), CompositeEvent(mintEvent)).item
            ?: throw IllegalStateException("Can't reduce item $itemId from event $entity")
    }
}
