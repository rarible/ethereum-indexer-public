package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import scalether.domain.Address

@ChangeLog(order = "00013")
class ChangeLog00013UpdateCryptoBots {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @ChangeSet(id = "ChangeLog00013UpdateCryptoBots.sendItemUpdates", order = "1", author = "protocol")
    fun sendItemUpdates(
        @NonLockGuarded itemRepository: ItemRepository,
        @NonLockGuarded reduceEventListenerListener: ReduceEventListenerListener
    ) = runBlocking<Unit> {
        val query = Query.query(Item::token isEqualTo Address.apply("0xf7a6e15dfd5cdd9ef12711bd757a9b6021abf643"))
        itemRepository.search(query).forEach {
            logger.info("Send notification for ${it.id}")
            reduceEventListenerListener.onItemChanged(it)
        }
    }
}
