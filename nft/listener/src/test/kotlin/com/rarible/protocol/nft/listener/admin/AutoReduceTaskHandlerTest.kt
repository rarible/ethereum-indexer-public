package com.rarible.protocol.nft.listener.admin

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.AutoReduce
import com.rarible.protocol.nft.core.repository.AutoReduceRepository
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.token.TokenReduceService
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
    private lateinit var itemReduceService: ItemReduceService

    @MockK
    private lateinit var tokenReduceService: TokenReduceService

    @MockK
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun run() = runBlocking<Unit> {
        val itemId1 = createRandomItemId()
        val itemId2 = createRandomItemId()
        coEvery { autoReduceRepository.findItems() } returns flowOf(
            AutoReduce(itemId1.toString()),
            AutoReduce(itemId2.toString())
        )
        val token1 = randomAddress()
        val token2 = randomAddress()
        coEvery { autoReduceRepository.findTokens() } returns flowOf(
            AutoReduce(token1.toString()),
            AutoReduce(token2.toString())
        )
        coEvery { autoReduceRepository.removeItem(AutoReduce(itemId1.toString())) } returns Unit
        coEvery { autoReduceRepository.removeItem(AutoReduce(itemId2.toString())) } returns Unit
        coEvery { autoReduceRepository.removeToken(AutoReduce(token1.toString())) } returns Unit
        coEvery { autoReduceRepository.removeToken(AutoReduce(token2.toString())) } returns Unit
        every {
            itemReduceService.update(
                token = itemId1.token,
                tokenId = itemId1.tokenId,
                from = itemId1,
                to = itemId1,
            )
        } returns Flux.just(itemId1)
        every {
            itemReduceService.update(
                token = itemId2.token,
                tokenId = itemId2.tokenId,
                from = itemId2,
                to = itemId2,
            )
        } returns Flux.just(itemId2)
        coEvery { tokenReduceService.reduce(token1) } returns null
        coEvery { tokenReduceService.reduce(token2) } returns null

        autoReduceTaskHandler.runLongTask(null, "")
    }
}
