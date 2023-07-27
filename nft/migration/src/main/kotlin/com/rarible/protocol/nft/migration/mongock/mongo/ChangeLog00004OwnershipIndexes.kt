package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00004")
class ChangeLog00004OwnershipIndexes {
    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.createIndexByOwner", order = "1", author = "aatapin")
    fun createIndexByOwner(template: MongockTemplate) {
        template.indexOps(OwnershipRepository.COLLECTION).ensureIndex(
            Index()
                .on(Ownership::owner.name, Sort.Direction.ASC)
                .on(Ownership::date.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }

    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.createIndexByCollection", order = "1", author = "aatapin")
    fun createIndexByCollection(template: MongockTemplate) {
        template.indexOps(OwnershipRepository.COLLECTION).ensureIndex(
            Index()
                .on(Ownership::token.name, Sort.Direction.ASC)
                .on(Ownership::date.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }

    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.deleteIndexByCreator", order = "1", author = "protocol")
    fun removeIndexByCreator(template: MongockTemplate) {
        /*
        Index()
            .on("creator", Sort.Direction.ASC)
            .on(Ownership::date.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
        */

        val indexes = template.indexOps(OwnershipRepository.COLLECTION).indexInfo

        indexes
            .find { it.name == "creator_1_date_1__id_1" }
            ?.let { template.indexOps(OwnershipRepository.COLLECTION).dropIndex(it.name) }
    }

    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.createIndexByCreators", order = "1", author = "protocol")
    fun createIndexByCreators(template: MongockTemplate) {
        template.indexOps(OwnershipRepository.COLLECTION).ensureIndex(
            Index()
                .on(Ownership::creators.name, Sort.Direction.ASC)
                .on(Ownership::date.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }

    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.createIndexByItem", order = "1", author = "aatapin")
    fun createIndexByItem(template: MongockTemplate) {
        template.indexOps(OwnershipRepository.COLLECTION).ensureIndex(
            Index()
                .on(Ownership::token.name, Sort.Direction.ASC)
                .on(Ownership::tokenId.name, Sort.Direction.ASC)
                .on(Ownership::date.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }

    @ChangeSet(id = "ChangeLog00004OwnershipIdexes.createIndexForAll", order = "1", author = "aatapin")
    fun createIndexForAll(template: MongockTemplate) {
        template.indexOps(OwnershipRepository.COLLECTION).ensureIndex(
            Index()
                .on(Ownership::date.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }
}
