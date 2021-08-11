package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.misc.div
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository.Companion.COLLECTION
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeLog(order = "00009")
class ChangeLog00009RemoveMintEvents {
    @ChangeSet(id = "ChangeLog00009RemoveMintEvents.remove", order = "001", author = "eugene")
    fun remove(mongo: MongockTemplate) {
        val criteria = LogEvent::data / ItemHistory::type isEqualTo "MINT"
        val deleted = mongo.remove(Query(criteria), COLLECTION).deletedCount
        logger.info("deleted $deleted")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00009RemoveMintEvents::class.java)
    }
}