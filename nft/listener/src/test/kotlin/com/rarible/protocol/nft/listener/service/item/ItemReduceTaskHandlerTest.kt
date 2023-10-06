package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.task.TaskRepository
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import scalether.domain.Address

internal class ItemReduceTaskHandlerTest {

    private val itemReduceService = mockk<ItemReduceService>() {
        every { update(any(), any(), any(), any(), any()) } returns Flux.empty()
    }

    private val taskRepository = mockk<TaskRepository>()

    private val itemReduceTaskHandler = ItemReduceTaskHandler(itemReduceService, taskRepository)

    @Test
    fun `should parse old param`() {
        itemReduceTaskHandler.runLongTask(
            from = ItemReduceState(token = Address.ONE(), tokenId = EthUInt256.ONE),
            param = Address.TWO().toString(),
        )
        verify {
            itemReduceService.update(
                token = null,
                tokenId = null,
                from = ItemId(token = Address.ONE(), tokenId = EthUInt256.ONE),
                to = ItemId(token = Address.TWO(), tokenId = EthUInt256.ZERO),
                updateNotChanged = false
            )
        }
    }

    @Test
    fun `should parse new param`() {
        itemReduceTaskHandler.runLongTask(
            from = ItemReduceState(token = Address.ONE(), tokenId = EthUInt256.ONE),
            param = "${Address.TWO()}:${EthUInt256.ONE}",
        )
        verify {
            itemReduceService.update(
                token = null,
                tokenId = null,
                from = ItemId(token = Address.ONE(), tokenId = EthUInt256.ONE),
                to = ItemId(token = Address.TWO(), tokenId = EthUInt256.ONE),
                updateNotChanged = false
            )
        }
        itemReduceTaskHandler.runLongTask(
            from = ItemReduceState(token = Address.ONE(), tokenId = EthUInt256.ONE),
            param = "${Address.TWO()}:10",
        )
        verify {
            itemReduceService.update(
                token = null,
                tokenId = null,
                from = ItemId(token = Address.ONE(), tokenId = EthUInt256.ONE),
                to = ItemId(token = Address.TWO(), tokenId = EthUInt256.TEN),
                updateNotChanged = false
            )
        }
    }

    @Test
    fun `should call for single item if range contains single item`() {
        itemReduceTaskHandler.runLongTask(
            from = ItemReduceState(token = Address.TWO(), tokenId = EthUInt256.ONE),
            param = "${Address.TWO()}:1",
        )
        verify {
            itemReduceService.update(
                token = Address.TWO(),
                tokenId = EthUInt256.ONE,
                from = null,
                to = null,
                updateNotChanged = false
            )
        }
    }
}
