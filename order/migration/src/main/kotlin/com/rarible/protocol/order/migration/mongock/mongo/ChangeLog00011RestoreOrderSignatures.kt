package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

// RPN-1135: restore fields of Order/OrderVersion
// @ChangeLog(order = "000011")
class ChangeLog00011RestoreOrderSignatures {

    private suspend fun <T, R : Comparable<R>> Flow<T>.maxBy(selector: (T) -> R): T? {
        var max: T? = null
        collect {
            if (max == null || selector(max!!) < selector(it)) {
                max = it
            }
        }
        return max
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun Order.updateSignature(version: OrderVersion): Order? {
        return when {
            signature == null && version.signature == null -> {
                logger.warn("Signature of order $hash is null and can't be recovered, setting to empty")
                copy(signature = Binary.apply())
            }
            signature == null && version.signature != null -> {
                logger.warn("Restoring signature of order ${hash}: was null, now will be ${version.signature}")
                copy(signature = version.signature)
            }
            signature != null && version.signature == null -> {
                // Nothing to restore, do not log.
                return null
            }
            signature != null && version.signature != null && signature != version.signature -> {
                logger.warn("Different signature in OrderVersion, skipping, in order: ${signature}, in version: ${version.signature}")
                return null
            }
            else -> {
                // Signatures in Order in OrderVersion are the same, skip, do not log.
                return null
            }
        }
    }

    // @ChangeSet(id = "ChangeLog00011RestoreOrderSignatures.restoreOrderSignatures", order = "1", author = "protocol")
    fun restoreOrderSignatures(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderVersionRepository: OrderVersionRepository
    ) = runBlocking {
        val orderRepository = MongoOrderRepository(template)

        logger.info("--- Start restoring 'signature' of Orders")
        var counter = 0L
        orderRepository.findAll().collect { order ->
            counter++
            if (counter % 5000L == 0L) {
                logger.info("Fixed $counter orders")
            }
            try {
                val lastVersion = orderVersionRepository.findAllByHash(order.hash).maxBy { it.id }
                lastVersion ?: return@collect
                try {
                    val updated = order.updateSignature(lastVersion) ?: return@collect
                    orderRepository.save(updated)
                } catch (_: Exception) {
                    optimisticLock {
                        val prevOrder = orderRepository.findById(order.hash) ?: return@optimisticLock
                        val updated = prevOrder.updateSignature(lastVersion) ?: return@optimisticLock
                        orderRepository.save(updated)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Can't fix order ${order.hash}")
            }
        }
        logger.info("--- All $counter order versions were updated")
    }
}
