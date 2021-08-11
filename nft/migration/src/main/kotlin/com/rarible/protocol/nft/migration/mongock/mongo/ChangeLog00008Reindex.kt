package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.subscribeAndLog
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import io.changock.migration.api.annotations.NonLockGuarded
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment

@ChangeLog(order = "00008")
class ChangeLog00008Reindex {
    @ChangeSet(id = "ChangeLog00001Reindex.reindex001", order = "001", author = "eugene")
    fun reindex001(@NonLockGuarded service: ItemReduceService, @NonLockGuarded env: Environment) {
        logger.info("reindexItems profile=${env.activeProfiles.toList()}")
        service.update(null, null)
            .then()
            .subscribeAndLog("reindex001", logger)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00008Reindex::class.java)
    }
}