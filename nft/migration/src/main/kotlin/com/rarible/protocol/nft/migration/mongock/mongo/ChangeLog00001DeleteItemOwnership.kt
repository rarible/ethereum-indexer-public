package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import org.springframework.data.mongodb.core.query.Query

@ChangeLog(order = "00001")
class ChangeLog00001DeleteItemOwnership() {

    @ChangeSet(id = "ChangeLog00001DeleteItemOwnership.deleteItem", order = "1", author = "eugene")
    fun deleteItem(template: MongockTemplate) {
        template.remove(Query(), Item::class.java)
    }

    @ChangeSet(id = "ChangeLog00001DeleteItemOwnership.deleteOwnership", order = "2", author = "eugene")
    fun deleteOwnership(template: MongockTemplate) {
        template.remove(Query(), Ownership::class.java)
    }

}