package com.rarible.protocol.erc20.migration.mongock

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues

@ChangeLog(order = "00003")
class ChangeLog00003RemoveUnusedData {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val unusedCollections = listOf(
        "log_check_state",
        "erc20_balance_snapshot",
        "state",
        "subscription"
    )

    private val tasksTypesToRemove = listOf(
        "TOPIC",
        "REVERT_WRONG_BLOCKS",
        "CHECK_WRONG_HASH",
        "ADMIN_REINDEX_ERC20_TOKENS",
        "INSERT_MISSING_BLOCKS",
        "ADMIN_BALANCE_REDUCE",
        "INSERT_MISSING_BLOCK_LOGS",
        "MONGO_CHANGE_erc20_balance"
    )

    @ChangeSet(
        id = "ChangeLog00003RemoveUnusedData.removeUnusedCollections",
        order = "1",
        author = "protocol",
        runAlways = false
    )
    fun removeUnusedCollections(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking {
        unusedCollections.forEach {
            template.dropCollection(it).awaitFirstOrNull()
        }
    }

    @ChangeSet(
        id = "ChangeLog00003RemoveUnusedData.removeLegacyTasks",
        order = "2",
        author = "protocol",
        runAlways = false
    )
    fun removeLegacyTasks(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking {
        val query = Query(Criteria("type").inValues(tasksTypesToRemove))
        template.remove(query, "task").awaitFirstOrNull()
    }
}
