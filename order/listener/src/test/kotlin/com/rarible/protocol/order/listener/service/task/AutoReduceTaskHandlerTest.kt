package com.rarible.protocol.order.listener.service.task

import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.model.AutoReduce
import com.rarible.protocol.order.core.repository.AutoReduceRepository
import com.rarible.protocol.order.core.repository.TempTaskRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.core.service.auction.AuctionReduceService
import com.rarible.protocol.order.listener.data.createOrder
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Flux

@ExtendWith(MockKExtension::class)
internal class AutoReduceTaskHandlerTest {
    @InjectMockKs
    private lateinit var autoReduceTaskHandler: AutoReduceTaskHandler

    @MockK
    private lateinit var taskRepository: TempTaskRepository

    @MockK
    private lateinit var auctionReduceService: AuctionReduceService

    @MockK
    private lateinit var orderReduceService: OrderReduceService

    @MockK
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun run() = runBlocking<Unit> {
        val auction1 = randomWord()
        val auction2 = randomWord()
        coEvery { autoReduceRepository.findAuctions() } returns flowOf(
            AutoReduce(auction1),
            AutoReduce(auction2)
        )
        val order1 = randomWord()
        val order2 = randomWord()
        coEvery { autoReduceRepository.findOrders() } returns flowOf(
            AutoReduce(order1),
            AutoReduce(order2)
        )
        coEvery { autoReduceRepository.removeAuction(AutoReduce(auction1)) } returns Unit
        coEvery { autoReduceRepository.removeAuction(AutoReduce(auction2)) } returns Unit
        coEvery { autoReduceRepository.removeOrder(AutoReduce(order1)) } returns Unit
        coEvery { autoReduceRepository.removeOrder(AutoReduce(order2)) } returns Unit
        every {
            auctionReduceService.update(Word.apply(auction1), Long.MAX_VALUE)
        } returns Flux.just(Word.apply(auction1))
        every {
            auctionReduceService.update(Word.apply(auction2), Long.MAX_VALUE)
        } returns Flux.just(Word.apply(auction2))
        every {
            orderReduceService.update(orderHash = Word.apply(order1))
        } returns Flux.just(createOrder())
        every {
            orderReduceService.update(orderHash = Word.apply(order2))
        } returns Flux.just(createOrder())

        autoReduceTaskHandler.runLongTask(null, "")
    }
}
