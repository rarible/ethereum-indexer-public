package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createRandomEthereumLog
import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.model.AutoReduce
import com.rarible.protocol.order.core.repository.AutoReduceRepository
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class AutoReduceServiceTest {
    @InjectMockKs
    private lateinit var autoReduceService: AutoReduceService

    @MockK
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun autoReduce() = runBlocking<Unit> {
        val auction1 = randomWord()
        val auction2 = randomWord()
        val order1 = randomWord()
        val order2 = randomWord()

        coEvery {
            autoReduceRepository.saveAuctions(setOf(AutoReduce(auction1), AutoReduce(auction2)))
        } returns Unit
        coEvery {
            autoReduceRepository.saveOrders(setOf(AutoReduce(order1), AutoReduce(order2)))
        } returns Unit

        autoReduceService.autoReduce(
            listOf(
                ReversedEthereumLogRecord(
                    id = randomString(),
                    version = randomLong(),
                    data = randomAuctionCreated().copy(hash = Word.apply(auction1)),
                    log = createRandomEthereumLog()
                ),
                ReversedEthereumLogRecord(
                    id = randomString(),
                    version = randomLong(),
                    data = randomAuctionCreated().copy(hash = Word.apply(auction2)),
                    log = createRandomEthereumLog()
                ),
                ReversedEthereumLogRecord(
                    id = randomString(),
                    version = randomLong(),
                    data = createOrderCancel().copy(hash = Word.apply(order1)),
                    log = createRandomEthereumLog()
                ),
                ReversedEthereumLogRecord(
                    id = randomString(),
                    version = randomLong(),
                    data = createOrderCancel().copy(hash = Word.apply(order2)),
                    log = createRandomEthereumLog()
                ),
            )
        )
    }
}
