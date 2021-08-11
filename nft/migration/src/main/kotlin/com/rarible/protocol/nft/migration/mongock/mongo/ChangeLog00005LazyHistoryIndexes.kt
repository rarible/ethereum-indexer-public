package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00005")
class ChangeLog00005LazyHistoryIndexes {

    @ChangeSet(id = "ChangeLog00002HistoryIndexes.createLazyHistoryIndexes", order = "1", author = "protocol")
    fun createHistoryIndexes(template: MongockTemplate) {
        template.indexOps(LazyNftItemHistoryRepository.COLLECTION).ensureIndex(
            Index()
                .on(ItemLazyMint::token.name, Sort.Direction.ASC)
                .on(ItemLazyMint::tokenId.name, Sort.Direction.ASC)
        )
    }
}