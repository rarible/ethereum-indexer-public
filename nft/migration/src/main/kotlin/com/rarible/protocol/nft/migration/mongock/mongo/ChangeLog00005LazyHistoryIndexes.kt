package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.rarible.protocol.nft.core.model.LazyItemHistory
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index

@ChangeLog(order = "00005")
class ChangeLog00005LazyHistoryIndexes {

    private val logger = LoggerFactory.getLogger(ChangeLog00005LazyHistoryIndexes::class.java)

    @ChangeSet(
        id = "ChangeLog00005LazyHistoryIndexes.createLazyNftItemHistoryRepositoryIndexes",
        order = "1",
        author = "protocol",
        runAlways = true,
    )
    fun extendTokenTokenIdIndexWithId(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        LazyNftItemHistoryRepository(template).createIndexes()
    }
}