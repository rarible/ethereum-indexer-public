package com.rarible.protocol.order.api.controller.admin

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderStateDto
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class AdminController(
    private val orderUpdateService: OrderUpdateService,
    private val orderService: OrderService,
    private val orderStateRepository: OrderStateRepository,
    private val orderDtoConverter: OrderDtoConverter,
) {
    @PostMapping(
        value = ["/admin/order/orders/{hash}/state"],
        produces = ["application/json"],
    )
    suspend fun cancelOrder(
        @PathVariable("hash") hash: String,
        @RequestBody orderStateDto: OrderStateDto
    ): ResponseEntity<OrderDto> {
        val order = orderService.get(hash.toOrderId().hash)
        val eventTimeMarks = orderOffchainEventMarks()
        return optimisticLock {
            val state = orderStateRepository.getById(order.hash)
                ?.withCanceled(orderStateDto.canceled)
                ?: OrderState.toState(order.hash, orderStateDto)

            orderStateRepository.save(state)
            orderUpdateService.update(order.hash, orderOffchainEventMarks())
            orderService.get(Word.apply(hash))
        }.let { ResponseEntity.ok(orderDtoConverter.convert(it)) }
    }
}
