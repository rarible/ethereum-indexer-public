package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index


@ChangeLog(order = "00003")
class ChangeLog00003ItemIndexes {
    @ChangeSet(id = "ChangeLog00003ItemIdexes.createIndexByOwner", order = "1", author = "aatapin")
    fun createIndexByOwner(template: MongockTemplate) {
        template.indexOps(ItemRepository.COLLECTION).ensureIndex(
            Index()
                .on(Item.Fields.OWNERS, Sort.Direction.ASC)
                .on(Item.Fields.DATE, Sort.Direction.DESC)
                .on(Item.Fields.ID, Sort.Direction.DESC)
        )
    }

    @ChangeSet(id = "ChangeLog00003ItemIdexes.createIndexByCollection", order = "2", author = "aatapin")
    fun createIndexByCollection(template: MongockTemplate) {
        template.indexOps(ItemRepository.COLLECTION).ensureIndex(
            Index()
                .on(Item.Fields.TOKEN, Sort.Direction.ASC)
                .on(Item.Fields.DATE, Sort.Direction.DESC)
                .on(Item.Fields.ID, Sort.Direction.DESC)
        )
    }

    @ChangeSet(id = "ChangeLog00003ItemIdexes.createIndexByItem", order = "3", author = "aatapin")
    fun createIndexByItem(template: MongockTemplate) {
        template.indexOps(ItemRepository.COLLECTION).ensureIndex(
            Index()
                .on(Item.Fields.TOKEN, Sort.Direction.ASC)
                .on(Item.Fields.TOKEN_ID, Sort.Direction.ASC)
                .on(Item.Fields.DATE, Sort.Direction.ASC)
                .on(Item.Fields.ID, Sort.Direction.ASC)
        )
    }

    @ChangeSet(id = "ChangeLog00003ItemIdexes.deleteIndexByCreator", order = "4", author = "protocol")
    fun removeIndexByCreator(template: MongockTemplate) {
        /*
        Index()
            .on("creator", Sort.Direction.ASC)
            .on(Item.Fields.DATE, Sort.Direction.DESC)
            .on(Item.Fields.ID, Sort.Direction.DESC)
        */
        val indexes = template.indexOps(ItemRepository.COLLECTION).indexInfo

        indexes
            .find { it.name == "creator_1_date_-1__id_-1" || it.name == "creator" }
            ?.let { template.indexOps(ItemRepository.COLLECTION).dropIndex(it.name)  }
    }

    @ChangeSet(id = "ChangeLog00003ItemIdexes.createIndexByRecipient", order = "5", author = "protocol")
    fun createIndexByRecipient(template: MongockTemplate) {
        template.indexOps(ItemRepository.COLLECTION).ensureIndex(
            Index()
                .on(Item.Fields.CREATORS_RECIPIENT, Sort.Direction.ASC)
                .on(Item.Fields.DATE, Sort.Direction.DESC)
                .on(Item.Fields.ID, Sort.Direction.DESC)
        )
    }

    @ChangeSet(id = "ChangeLog00003ItemIdexes.createIndexForAll", order = "6", author = "aatapin")
    fun createIndexForAll(template: MongockTemplate) {
        template.indexOps(ItemRepository.COLLECTION).ensureIndex(
            Index()
                .on(Item.Fields.DATE, Sort.Direction.DESC)
                .on(Item.Fields.ID, Sort.Direction.DESC)
        )
    }
}
