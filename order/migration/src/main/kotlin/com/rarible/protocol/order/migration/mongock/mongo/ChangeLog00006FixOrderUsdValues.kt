package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.misc.toDto
import com.rarible.protocol.order.core.model.OrderUsdValue
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.util.UUID

@ChangeLog(order = "00007")
class ChangeLog00006FixOrderUsdValues {
    @ChangeSet(id = "ChangeLog00006FixOrderUsdValues.fixOrdersUsdValues", order = "1", author = "protocol")
    fun createIndexForAll(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded orderVersionRepository: OrderVersionRepository,
        @NonLockGuarded priceUpdateService: PriceUpdateService,
        @NonLockGuarded publisher: ProtocolOrderPublisher,
        @NonLockGuarded orderDtoConverter: OrderDtoConverter
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)
        val orderRepository = MongoOrderRepository(template)

        logger.info("--- Start fix orders usd values")
        var counter = 0L

        orderRepository.findAll().collect { order ->
            try {
                val usdValue = priceUpdateService.getAssetsUsdValue(order.make, order.take, nowMillis())

                if (usdValue != null) {
                    try {
                        template.save(order.withOrderUsdValue(usdValue)).awaitFirst()
                    } catch (_: OptimisticLockingFailureException) {
                        optimisticLock {
                            orderRepository.findById(order.hash)
                                ?.let { template.save(it.withOrderUsdValue(usdValue)).awaitFirst() }
                        }
                    }

                    orderVersionRepository.findAllByHash(order.hash).collect { version ->
                        try {
                            orderVersionRepository.save(version.withOrderUsdValue(usdValue)).awaitFirst()
                        } catch (_: OptimisticLockingFailureException) {
                            optimisticLock {
                                orderVersionRepository.findById(version.id).awaitFirstOrNull()
                                    ?.let {
                                        orderVersionRepository.save(it.withOrderUsdValue(usdValue))
                                    }
                            }
                        }
                    }
                }
                if (counter % 50000L == 0L) {
                    logger.info("Fixed $counter orders")
                }
                counter++
            } catch (ex: Exception) {
                logger.error("Can't fix order ${order.id}")
            }
        }
        orderRepository.findActive().collect { order ->
            try {
                val updateEvent = OrderUpdateEventDto(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.id.toString(),
                    order = orderDtoConverter.convert(order),
                    eventTimeMarks = orderOffchainEventMarks().toDto()
                )
                publisher.publish(updateEvent)

                counter++

                if (counter % 50000L == 0L) {
                    logger.info("Published $counter events")
                }
            } catch (ex: Exception) {
                logger.error("Can't publish event for ${order.id}")
            }
        }

        logger.info("--- All $counter orders were fixes")
    }

    private fun OrderVersion.withOrderUsdValue(usdValue: OrderUsdValue): OrderVersion {
        return copy(
            makePriceUsd = usdValue.makePriceUsd,
            takePriceUsd = usdValue.takePriceUsd,
            makeUsd = usdValue.makeUsd,
            takeUsd = usdValue.takeUsd
        )
    }
}
