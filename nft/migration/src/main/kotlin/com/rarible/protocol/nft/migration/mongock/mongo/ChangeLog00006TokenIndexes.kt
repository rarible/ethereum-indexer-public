package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.Token
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00006")
class ChangeLog00006TokenIndexes {
    @ChangeSet(id = "ChangeLog00006TokenIndexes.createIndexByOwnerAndStandard", order = "1", author = "protocol")
    fun createIndexByOwner(template: MongockTemplate) {
        template.indexOps(Token::class.java).ensureIndex(
            Index()
                .on(Token::owner.name, Sort.Direction.ASC)
                .on(Token::standard.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
    }
}